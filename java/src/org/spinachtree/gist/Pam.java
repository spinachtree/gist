
package org.spinachtree.gist;

class Pam {
	
	//  Parser Abstract machine .............. aka Peter's abstract machine.......
	
	//  Pam uses a simple integer opcode instructions for speed and portablility to other languages,
	//  and to provide an interface for low level embedded parser machine implementations...
	
	// The parse run time performace is largely determined here, but the grammar compiler should
	// optimize the Pam opcodes it generates from the grammar rules.
	
	// Pam generates a syntax tree in the form of a Span index structure to label substrings.
	// This enables a simple fast parse engine, but assumes indexed access to the source text,
	// which seems natural for a String or character array, but not so good for a linked list of char codes.
	// Also an index depends on the character size, be it 8-bit for ASCII, 16-bit char (eg Java) or
	// a 32 bit int (>20 bits covers all Unicodes).
	
	// The Span index structure is used as an interal representation, the external representation of
	// the parse tree terms is designed to suit the particular host applicatin programming language. 
	 
	// The Pam syntax tree Span index records are immutable, they are never re-written.
	
	//  Pam opcode instructions:
	//  -- 32-bit int  << op:8 , arg:24 >> 
	//  -- max op code < 64 so a coda will fit into a 30-bit integer
	//  -- op > 0, so that naked values < 24 bits in code[] are distinct from opcodes

	static final int  // basic op code instructions, sufficient for bootstrap .....
	REF=1,  // exec: rule, arg=rule-id, success generates a rule-id syntax tree term
	RUN=2,  // exec: op, arg -> op, ie op=code[arg], elide.. no syntax tree term
	SEQ=3,  // sequence: x y ..., arg -> [n,ops...] 
	SEL=4,  // select: first match x y ..., arg -> [n,ops...] 
	REP=5,  // repeat: x* arg -> op
	RAP=6,  // repeat: x+ arg -> op
	OPT=7,  // option: x? arg -> op
	CHS=8;  // char-set match: arg -> [n, a,z, ... ranges]
	
	static final int  // extra op code instructions for full PBNF grammar rules...
	NOT=9,   // !x
	ISA=10,  // &x => !!x
	ALT=11,  // x | y  -- longest match
	EXT=12,  // x.y [or x default] external grammar rule link <<EXT:8, RUN|REF:4, Link#:10, Rule#:10>>
	NOP=13,  // '' empty string
	PAT=14,  //  @name -- PRE predicate prior name exists...
	PEQ=15;  //  @=name -- PEQ find prior name span and match same again
	
	static final int  // experimental op code instructions...	
	PEC=33,  //  @: item -- PEC match last prior char
	LST=34,  // S* = ... => generate syntax tree term only for list of two or more children
	ACT=35;  // S^ = ... => action event interface

	// == interface methods  ===================================================================================

	// encode op code instructions << op:8, arg:24 >>  ..................................

	static int opcode(int op, int arg) { return (op<<24 | arg); }
	
	// EXT  <<EXT:8, RUN/REF:4, link:10, tag:10 >>
	static int extRun(int link, int tag) { return (EXT<<24 | RUN<<20 | (link&0x3ff)<<10 | tag&0x3ff); }
	static int extRef(int link, int tag) { return (EXT<<24 | REF<<20 | (link&0x3ff)<<10 | tag&0x3ff); }

	// decode: << op:8, arg:24 >> .......................................................

	static int op(int coda) { return coda>>24; }

	static int arg(int coda) { return coda&0xFFFFFF; }

	// opcode names -- only needed by CodePad for debug dump, not used in normal operation......
	
	static final String[] opNames =
	{ "_", "REF", "RUN", "SEQ", "SEL", "REP", "RAP", "OPT", "CHS",  // 0..8
	 "NOT", "ISA", "ALT", "EXT", "NOP", "PAT", "PEQ", "PEC", "LST", "ACT" }; // 9..18

	// yes, an enum would normally be a much better solution, but it didn't work out as well as I'd like...
		
	/* == code[] layout ======================================================================================
	
		code[0..N]			opcodes for N rule definitions
		code[N+1...M]			opcodes and data implementing the grammar rules....
		code[code.length-2]		N+1, rule count
		code[code.length-1]		M+1, size of loaded code

	*/
	
	// == Pam ================================================================================================
	
	Pam(String input, int[] code, Pam[] links) {
		this.input=input;
		this.limit=input.length();
		this.code=code;
		this.ruleCount=code[code.length-2];
		this.links=links;
		pos=0;
		max=0;
		tree=Span.NIL;  // seed
		calls=new int[ruleCount];
		for (int i=0;i<ruleCount;i++) calls[i]=-1;
		memos=new Span[ruleCount];
		loops=new Span[ruleCount];
		looping=-1;
	}	

	int[] code;	// grammar program opcodes, 1..N rule definitions, more code...
	int ruleCount;
	
	Pam[] links; // sub-parsers
	
	String input;
	int pos;      // input cursor index
	int max;      // peak pos [fail point]
	int limit;    // end of input 

	Span tree;   // output
	
	int[] calls;  // call pos
	Span[] memos; // grit lists
	Span[] loops; // left recursions
	int looping;  // looper rule# (init -1)
		
	boolean parse(int rule) {
		return callRef(0,rule,this);
	}

	boolean callRun(int link, int rule, Pam caller) {
		pos=caller.pos;
		max=caller.max;
		tree=caller.tree; 
		boolean result=exec(code[rule]);
		if (result) {
			caller.pos=pos;
			caller.max=max;
			caller.tree=tree;
		} else caller.max=max;
		return result;
	}

	boolean callRef(int link, int rule, Pam caller) {
		pos=caller.pos;
		max=caller.max;
		tree=caller.tree; 
		boolean result=exec(REF<<24|(link<<10)|rule);
		if (result) {
			caller.pos=pos;
			caller.max=max;
			caller.tree=tree;
		} else caller.max=max;
		return result;
	}
		
	boolean exec(int coda) {
		if (coda<0xffffff) coda=code[coda];
		int op=coda>>24;
		int arg=coda&0xFFFFFF;
		
		int sot;  // start of text
		Span tip; // tree tip
		
		int nest; // last call pos

		int i, n; // code index and size
		int j, k; // misc local vars
		int link,rule; // REF arg=<<link,rule>>
		int best;  // best ALT pos
		Span beat; // best tree
		boolean result;
		Span x,y, memo;
		 
		switch(op) {
			// -- PEG opcodes ---------------------------------------------------
			
			case REF:	// arg=<<link#:10, rule#:10>>, rule# 1..N
				link=arg>>10;
				rule=arg&0x3ff;
				// check for a memo.......
				memo=memos[rule];  // last result for this rule
				if (memo!=null && memo.sot>=pos && applyMemo(rule,memo)) return true;
				// check for a left recusive loop.............................
				nest=calls[rule];
				if (nest==pos) return looped(rule); // loop -> fail
				calls[rule]=pos;   // save input index for this call
				// .....no memo or left recursion, normal parse..................
				sot=pos; tip=tree; // save the current sate
				result=exec(rule);  // run the rule....
				calls[rule]=nest;  // all done at this pos (may fail back to it) 
				if (result) {
					if (max<pos) max=pos; // record for fault reporting
					tree=newSpan(arg,sot,pos,tip,tree);
				}
				if (loops[rule]!=null) leftRecursion(result,arg,rule,sot,tip); // loop
				return result;

			case RUN:	// arg=rule#, no checks, no memos, just run it...
				return exec(arg);

			case SEQ:	// arg=code ptr to opcodes: x,y,z....
				n=code[arg]; i=arg+1; // n=opcode count, i ptr to first
				while (i<=arg+n) if (!exec(i)) return false; else i++;
				return true;

			case SEL:	// arg=code ptr to opcodes: x/y/z....
				sot=pos; tip=tree; // save start state
				n=code[arg]; i=arg+1; // n=opcode count, i ptr to first
				while (i<=arg+n) {
					if (exec(i)) return true; // first match result
					pos=sot; tree=tip; // reset to start state
					i++; // try next opcode
				} // nothing matched	
				return false;

			case RAP: // arg+
				if (!exec(arg)) return false;
			case REP: // arg*
				while (true) {
					sot=pos; tip=tree;
					if (!exec(arg) || pos==sot) break;
				}
				pos=sot; tree=tip; // reset
				return true;

			case OPT: // arg?
				sot=pos; tip=tree;
				if (exec(arg) && pos>sot) return true;
				pos=sot; tree=tip; // reset
				return true;

			case CHS:
				if (pos>=limit) return false; // end of input
				int ch=input.charAt(pos); // ascii version
				int bot=arg+1; // min of lowest range: n..m
				int top=bot+code[arg]; //  min of highest range
				while (bot<top) { // binary-chop search
					int pick=bot+((top-bot)>>2<<1); // middle
					if (ch<code[pick]) top=pick; // below min, chop ranges here and above
					else if (ch>code[pick+1]) bot=pick+2; // above max, chop here and below
					else { // match ch>=n && ch<=m
						pos=pos+1; // advance char width, ascii=1
						if (max<pos) max=pos;
						return true;
					}
				}
				return false;
			
			case NOP: return true; // '' empty match
				
			// -- MEG opcodes ------------------------------------------------------------
			
			case ALT:	// arg=code ptr to opcodes: x|y|z....
				sot=pos; tip=tree; // save start state
				n=code[arg]; i=arg+1; // n=opcode count, i ptr to first
				best=-1; beat=null;
				while (i<=arg+n) {
					if (exec(i) && pos>best) { best=pos; beat=tree; }
					pos=sot; tree=tip; // reset to start state
					i++; // try next opcode
				}
				if (best<0) return false; // nothing matched
				pos=best; tree=beat;
				return true;
			
			case EXT:  //  << EXT:8, RUN/REF:4, link:10, tag:10 >>
				i=(arg>>20)&0xf;   // RUN | REF
				j=(arg>>10)&0x3ff; // Pam link#
				k=arg&0x3ff;       // tag rule#
				if (i==RUN) return links[j].callRun(j,k,this);
				else return links[j].callRef(j,k,this);
				
			case NOT: // !x
				sot=pos; tip=tree; // save start state
				result=exec(arg);
				pos=sot; tree=tip; // reset
				return !result;
				
			case ISA: // &x
				sot=pos; tip=tree; // save start state
				result=exec(arg);
				pos=sot; tree=tip; // reset
				return result;

			case PAT: //  @x -- PRE predicate, prior x exists...
			case PEQ: //  @=x -- PEQ find prior name span and match same again
				x=tree;
				y=Span.NIL;
				n=code[arg]; i=arg+1; // n=name count, i ptr to first
				while (x!=null) {     // name.name.name...
					if (x==y) return false;
					if (x.tag==code[i]) {
						if (n==1)
							if (op==PAT) return true;
							else { // PEQ @= match....
								n=x.eot-x.sot;
								if (pos+n>limit) return false; // short input
								String xs=input.substring(x.sot,x.eot);
								String is=input.substring(pos,pos+n);
								if (!is.equals(xs)) return false;
								pos=pos+n; // advance char width, ascii=1
								if (max<pos) max=pos;
								return true;
							}
						n--; i++;
						y=x.tip;
						x=x.top;
					} else x=x.tip;
				}
				return false;
				
			case PEC: //   @: item -- PEC predicate, true if item matches last prior char..
				if (pos==0) return false;
				sot=pos; tip=tree; // save start state
				pos=pos-1; // step back one char
				result=exec(arg);
				pos=sot; tree=tip; // reset
				return result;

			// -- experimental opcodes ---------------------------------------------------
				
			case LST:	// rule* = ... 
				// only generates a parent rule if there are 2 or more children
				// arg=rule#, no loop check, no memo
				sot=pos; tip=tree;
				if (!exec(arg)) return false;
				if (max<pos) max=pos;
				if (tree.tip!=tip)  tree=newSpan(arg,sot,pos,tip,tree);
				return true;

		}
		System.out.printf("Pam: unknown opcode: %d\n",op);
		System.exit(-1); // unknown opcode
		return false;
		
	} // exec
	
	Span newSpan(int arg, int sot, int pos,Span tip, Span top) {
		int rule=arg&0x3ff;
		Span grit=new Span(arg,sot,pos,tip,top,memos[rule]);
		memos[rule]=grit; // list of results for this rule
		return grit;
	}
	
	// // -- Match ---------
	// 
	// boolean matchPrior(int pos, Span x) {
	// 	int ch=input.charAt(pos); // ascii version
	// 	
	// 	return false;
	// }

	// -- Memos ---------------------------------------------------------------
	
	boolean applyMemo(int rule, Span memo) {
		do {  // possible memo......
			if (memo.sot==pos) { // apply this memo.....
				if (looping>-1 && looping!=rule) return false; // indirect loop transit rule
				if (memo.tip==tree) tree=memo; // same again
				else return false; //tree=memo.copy(memo.tip,tree); // new result linked to tree
				pos=memo.eot;
				return true;
			}  // try previous memo...
			memo=memo.link;
		} while (memo!=null && memo.sot<pos);
		return false; // no memo...
	}
	
	// -- Left Recursion -----------------------------------------------------

	boolean looped(int rule) { // left recursion loop....
		Span loop=loops[rule]; // may be nested loops  
		while (loop!=null) {
			if (loop.sot==pos) return false; // deja vue, again
			loop=loop.link;
		}
		loops[rule]=new Span(rule,pos,0,null,null,loops[rule]);
		return false;
	}

	void leftRecursion(boolean result, int arg, int rule, int sot, Span tip) {
		if (result) leftLoop(arg,rule,sot,tip); // build on seed
		Span loop=loops[rule]; 
		while (loop!=null) { // delete loop @ sot
			if (loop.sot==sot) loops[rule]=loop.link;
			loop=loop.link;
		}
	}

	void leftLoop(int arg, int rule, int sot, Span tip) {
		int looper=looping;
		looping=rule;
	 	int best=pos; Span top=tree;
		while (true) { // repeat loop, builds nested results...
			best=pos; top=tree; // save best so far..
			pos=sot; tree=tip; // reset to run again
			boolean result=exec(rule);  // run the rule.... ref uses memo
			if (!result || pos<=best) break;
			tree=newSpan(arg,sot,pos,tip,tree);
		}
		pos=best; tree=top; // reset to last success
		if (max<pos) max=pos; // record for fault reporting
		looping=looper;
	}


} // Pam

