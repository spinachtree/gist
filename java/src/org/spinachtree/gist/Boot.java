
package org.spinachtree.gist;

class Boot {
	
	/* 
	Bootstrap grammar:
	-- rich enough to allow the full Gist grammar to be easily expressed.
	-- `elide is included to simplify the compiler
	-- but small enough to be hand compiled.
	*/

	static final String bootGrammar = 
	"rules   = (rule/s)*                  \n"+
	"rule    = head s sel                 \n"+
	"head    = name star? s defn          \n"+
	"sel     = seq (or seq)*              \n"+
	"seq     = rep (and rep)*             \n"+
	"rep     = item repn?                 \n"+
	"item    = ref/code/quote/group       \n"+
	"group   = '(' s sel s ')'            \n"+
	"code    = int ('.' '.' int)?         \n"+
	"quote   = quo ('.' '.' quo)?         \n"+
	"ref     = elide? name                \n"+
	"quo     : 39 (32..38/40..126)* 39    \n"+
	"name    : ('A'..'Z'/'a'..'z')+       \n"+
	"int     : '0'..'9'+                  \n"+
	"repn    : '*'/'+'/'?'                \n"+
	"defn    : '='/':'                    \n"+
	"star    : '*'                        \n"+
	"elide   : '`'                        \n"+
	"or      : s '/' s                    \n"+
	"and     : (9/32)+                    \n"+
	"s       : 9..32*                     \n";

	// code block: 0..N rules, opcodes and data... code[SIZE-1]==actual load size.
	static final int SIZE=256;
	
	static final String[] ruleNames =
		{"rules", "rule", "head", "sel", "seq", "rep", "item", "group", "code", "quote",
		  "ref", "quo", "name", "int", "repn", "defn", "star", "elide", "or", "and", "s" };
	
	static Parser parser() { // compiler result...
		return new Parser(bootGrammar,ruleNames,new Boot().code);
	}

	Boot() { // hand compiled opcodes for booot grammar.........
		def("rules",rep(sel(_rule,_s)));
		def("rule",seq(_head,_s,_sel));
		def("head",seq(_name,opt(_star),_s,_defn));
		def("sel",seq(_seq,rep(seq(_or,_seq))));
		def("seq",seq(_rep,rep(seq(_and,_rep))));
		def("rep",seq(_item,opt(_repn)));
		def("item",sel(_ref,_code,_quote,_group));
		def("group",seq(chs(40),_s,_sel,_s,chs(41)));
		def("code",seq(_int,opt(seq(chs(46),chs(46),_int))));
		def("quote",seq(_quo,opt(seq(chs(46),chs(46),_quo))));
		def("ref",seq(opt(_elide),_name));
		// leaf rules.....
		def("quo",seq(chs(39),chs(32,38,40,126),chs(39)));
		def("name",rap(chs(65,90,95,95,97,122)));
		def("int",rap(chs(48,57)));
		def("repn",chs(42,43,63,63));
		def("defn",sel(chs(58),chs(61)));
		def("star",chs(42));
		def("elide",chs(96));
		def("or",seq(_s,chs(47),_s));
		def("and",rap(chs(9,9,32,32)));
		def("s",rep(chs(9,32)));
		// load length	
		code[code.length-2]=ruleNames.length;	
		code[code.length-1]=loadAt;
	}

	int[] code=new int[256]; // the compiler needs to calculate (or build) the size needed
	int loadAt=ruleNames.length; // must start code loading > code[1..N] rule defs
	
	void def(String rule,int body) { code[tag(rule)]=body; }

	int run(int op) {return Pam.opcode(Pam.RUN,load(op));}
	int ref(int op) {return Pam.opcode(Pam.REF,load(op));}
	int rep(int op) {return Pam.opcode(Pam.REP,load(op));}
	int rap(int op) {return Pam.opcode(Pam.RAP,load(op));}
	int opt(int op) {return Pam.opcode(Pam.OPT,load(op));}
	
	int seq(int... ops) { return Pam.opcode(Pam.SEQ,load(ops)); }
	int sel(int... ops) { return Pam.opcode(Pam.SEL,load(ops)); }
	
	int chs(int... xs) {
		if (xs.length==1) return chs(xs[0],xs[0]);
		return Pam.opcode(Pam.CHS,load(xs));
	}
	
	int load(int x) {
		int pc=loadAt;
		code[loadAt++]=x;
		return pc;
 	}

	int load(int[] xs) {
		int pc=loadAt;
		code[loadAt++]=xs.length;
		for (int x:xs) code[loadAt++]=x;
		return pc;
 	}
	
	// rule name calls:
	// _name used for a REF or RUN call (literals)
	// prefix disambiguates rule name calls from boot code identifiers

	static int tag(String rule) {
		for (int i=0; i<ruleNames.length; i++) {
			if (ruleNames[i].equals(rule)) return i;
		}
		System.out.println("Boot? tag="+rule); // woops
		return -1;
	}

 	static final int
	_rules = Pam.opcode(Pam.REF,tag("rules")),
	_rule  = Pam.opcode(Pam.REF,tag("rule")),
	_head  = Pam.opcode(Pam.REF,tag("head")),
	_sel   = Pam.opcode(Pam.REF,tag("sel")),
	_seq   = Pam.opcode(Pam.REF,tag("seq")),
	_rep   = Pam.opcode(Pam.REF,tag("rep")),
	_item  = Pam.opcode(Pam.REF,tag("item")),
	_group = Pam.opcode(Pam.REF,tag("group")),
	_code  = Pam.opcode(Pam.REF,tag("code")),
	_quote = Pam.opcode(Pam.REF,tag("quote")),
	_ref   = Pam.opcode(Pam.REF,tag("ref")),
	_quo   = Pam.opcode(Pam.REF,tag("quo")),
	_name  = Pam.opcode(Pam.REF,tag("name")),
	_int   = Pam.opcode(Pam.REF,tag("int")),
	_repn  = Pam.opcode(Pam.REF,tag("repn")),
	_defn  = Pam.opcode(Pam.REF,tag("defn")),
	_star  = Pam.opcode(Pam.REF,tag("star")),
	_elide = Pam.opcode(Pam.REF,tag("elide")),
	_or    = Pam.opcode(Pam.RUN,tag("or")),
	_and   = Pam.opcode(Pam.RUN,tag("and")),
	_s     = Pam.opcode(Pam.RUN,tag("s"));

} //Boot
