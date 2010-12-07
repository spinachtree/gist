
package org.spinachtree.gist;

import java.util.*;

class Compiler {

	static Parser compile(String gist, Parser parser) {
		return new Compiler(gist,parser,null,null).compile();
	}
	
	static Parser compile(String gist, Parser parser, Map<String,Gist> glib, List<String> defaults) {
		return new Compiler(gist,parser,glib,defaults).compile();
	}
	
	String gist; // the source grammar to be compiled
	Parse gistParse; // source parse tree
	String[] gistNames; 
	int ruleCount;

	Span[] ruleStates; // rule# -> parse tree span
	int[] ruleAtts;   // gist rules: name star? defn -> STAR LEAF 
	Map<String,Integer> ruleIdx;  // gistName -> rule#

	CodePad codePad; // support for generating opcodes

	Parser parser; // gist grammar rule parser

	
	Compiler(String gist, Parser parser, Map<String,Gist> glib, List<String> defaults) {
		this.gist=gist; // new gist grammar rules
		this.parser=parser; // gist grammar rule parser
		this.glib=glib; // external grammar library (may be null)
		this.defaults=defaults; // glib names keys to search for undefined names
		linkMap=null; // actual external links needed
	}
	
	Parser compile() {
		gistParse=parser.parse(gist);
		//System.out.println(gistParse);
		if (!gistParse.isOK()) throw new IllegalArgumentException(gistParse.toString());
		int ruleTag=parser.tag("rule");
		gistNames=ruleNames(gistParse,ruleTag,gist);
		ruleCount=gistNames.length;
		ruleStates=new Span[ruleCount];
		Span[] rs=gistParse.span.children(ruleTag);
		ruleIdx=new HashMap<String,Integer>();
		for (int i=0;i<ruleCount;i++) { // gistName -> ruleIdx
			ruleIdx.put(gistNames[i],new Integer(i));
			ruleStates[i]=rs[i]; // parse tree span
		}
		ruleAtts=new int[ruleCount];
		codePad=new CodePad(gistNames);
		for (Span r:rs) rule(r);
		return new Parser(gist,gistNames,ruleIdx,codePad.code(),parserLinks());
	}

	static String[] ruleNames(Parse parse, int ruleTag, String gist) {
		Span[] rs=parse.span.children(ruleTag);
		int n=rs.length;
		String[] names=new String[n];
		int i=0; // rule names 0..N-1
		for (Span rule:rs) { // rule = head xs alt
			Span name=rule.child().child(); // head = name defn
			names[i++]=gist.substring(name.sot,name.eot).intern();
		}		
		return names;
	}
	
	int ruleTag(String name) {
		Integer id=ruleIdx.get(name);
		if (id==null) return -1;
		return id.intValue();
	}
	
	// Meta programming needed...  Introspection works fine, but it
	// is too complicated; so keep it simple and just spell it out...
	
	int compile(Span x, int host) { // tag -> String -> method
		String rule=parser.ruleNames[x.tag];
		// System.out.printf("compile: sot=%d rule=%s \n",x.sot,rule);
		if (rule=="ref") return ref(x,host);
		if (rule=="alt") return compileList(Pam.ALT,x,host);
		if (rule=="sel") return compileList(Pam.SEL,x,host);
		if (rule=="seq") return compileList(Pam.SEQ,x,host);
		if (rule=="rep") return rep(x,host);
		if (rule=="elem") return elem(x,host);
		if (rule=="item") return compile(x.child(),host);
		if (rule=="prime") return compile(x.child(),host);
		if (rule=="group") return compileContent(x,host);
		if (rule=="not") return opload(Pam.NOT,compile(x.last(),host));
		if (rule=="isa") return opload(Pam.ISA,compile(x.last(),host));
		if (rule=="pre") return compile(x.child(),host);
		if (rule=="pat") return pat(x,host);
		if (rule=="pec") return opload(Pam.PEC,compile(x.last(),host));
	////	if (rule=="pan") return opload(Pam.PAN,compile(x.last(),host));
		if (rule=="code") return code(x,host);
		if (rule=="ints") return code(x,host);
		if (rule=="hexs") return hexs(x,host);
		if (rule=="quote") return quote(x,host);
		if (rule=="quo") return quo(x);
		if (rule=="int") return chs(integer(x));
		if (rule=="rule") return rule(x);
		if (rule=="elide") return chs(96);
		if (rule=="w"||rule=="s"||rule=="h"||rule=="and"||rule=="or") return -1;
		if (rule=="many") return opload(Pam.REP,compileContent(x,host));
		if (rule=="option") return opload(Pam.OPT,compileContent(x,host));
		woops("compile? rule="+rule);
		return -1; // invalid
	}

	int compileList(int opcode, Span x, int host) { 
		//  ops = x x*  => x|y|z.. or x/y/z  or x,y,z 
		Span[] args=x.children();
		int i,j=0,n=args.length;
		if (n==1) return compile(args[0],host);
		int[] ops=new int[n];
		for (i=0;i<n;i++) {
			int op=compile(args[i],host);
			if (op<0) continue;
			if (opcode==Pam.SEQ) {
				if (i>0 && isChs(op) && isNotChs(ops[j-1]))
					ops[j-1]=excludeChs(op,ops[j-1]);
				else ops[j++]=op;
			} else { // ALT | SEL
				if (i>0 && isChs(op) && isChs(ops[j-1]))
					ops[j-1]=mergeChs(ops[j-1],op);
				else ops[j++]=op;
			}
		}
		if (j==1) return ops[0];
		return Pam.opcode(opcode,codePad.load(ops,j));
	}
	
	// ruleStates[rule#] state:  parse_tree_grit -> PENDING -> COMPILED

	static final Span PENDING=new Span(0,0,0,null,null,null);
	static final Span COMPILED=new Span(0,0,0,null,null,null);
		
	boolean isCompiled(int rid) {
		Span state=ruleStates[rid];
		return (state==COMPILED);
	}

	boolean isPendingOrCompiled(int rid) {
		Span state=ruleStates[rid];
		return (state==COMPILED || state==PENDING);
	}
		
	void woops(String msg) {
		System.out.println(msg);
		System.exit(-1);
	}
	
	// rule atts .................
	
	int LEAF=1, STAR=2;
	
	boolean isLeaf(int rule) { return ((ruleAtts[rule]&LEAF)==LEAF); }

	boolean isStar(int rule) { return ((ruleAtts[rule]&STAR)==STAR); }

	// == compile Span tree -> opcodes ====================================================

	// rule methods.........................
	
	int rule(Span ruleSpan) {
		// rule = head sel
		// head = name star? defn
		Span[] heads=ruleSpan.child().children();
		int n=heads.length;
		String name=text(heads[0]);
		int rule=ruleTag(name);
		if (rule<0) woops("missing rule: "+name);
		if (isPendingOrCompiled(rule)) return rule;		
		if (isTag("star",heads[1])) ruleAtts[rule]|=STAR;
		if (isText(":",heads[n-1])) ruleAtts[rule]|=LEAF;
		ruleStates[rule]=PENDING; // being compiled
		codePad.load(rule,compile(ruleSpan.last(),rule));
		ruleStates[rule]=COMPILED;
		return rule;
	}

	int ref(Span x, int host) {
		// ref = elide? name ('.' name)*
		Span[] args=x.children();
		int n=args.length;
		String name=text(args[n-1]);
		Boolean elide=isTag("elide",args[0]);
		//Boolean elide=(name.endsWith("_")||name.equals("~")||name.equals("$"));
		if ((!elide && n>1) || (elide && n>2)) { // name.name...
			int name1= (elide? 1:0);
			String label=gist.substring(args[name1].sot,args[n-2].eot);
			int ext=findExternal(elide,label,name,host);
			if (ext>=0) return ext;
			undefinedRule(label+"."+name,ext);
			return ext;
		}
		int rule=ruleTag(name);
		if (rule==-1) {
			int ext=findDefault(elide,name,host); 
			if (ext>=0) return ext;
			undefinedRule(name,ext);
			return ext;
		}
		if (!isPendingOrCompiled(rule)) rule(ruleStates[rule]); // compile the target rule...
		if (isCompiled(rule)) {
			if (isLeaf(host) || elide) {
				int body=ruleBody(rule);
				if (isChs(body)) return body;
				return Pam.opcode(Pam.RUN,rule);
			} else if (isStar(rule)) return Pam.opcode(Pam.LST,rule);
		}
		if (isStar(rule)) return Pam.opcode(Pam.LST,rule);
		if (elide) return Pam.opcode(Pam.RUN,rule);
		return Pam.opcode(Pam.REF,rule);
	}

	void undefinedRule(String name, int ext) {
		if (ext==-1) woops("undefined rule: "+name+" no definition found...");
		else if (ext==-2) woops("undefined rule: "+name+" grammar not found in library...");
		else woops("undefined rule: "+name);
	}

	int rep(Span x, int host) { 
		// rep = fx repn?
		Span[] args=x.children();
		int fx=compile(args[0],host);
		if (args.length==1) return fx;
		String repn=text(args[1]);
		if (repn.equals("*")) return opload(Pam.REP,fx);
		if (repn.equals("+")) return opload(Pam.RAP,fx);
		if (repn.equals("?")) return opload(Pam.OPT,fx);
		woops("rep? repn="+repn);
		return -1; // invalid
	}

	int compileContent(Span x, int host) { 
		// group (etal) = '(' w alt w ')' or '(' exp ')' etal
		Span[] spans=x.children();
		int n=spans.length;
		if (n==1) return compile(spans[0],host);
		if (n==3) return compile(spans[1],host);
		woops("compileContent? "+text(x));
		return -1;
	}

	int elem(Span x, int host) { 
		//  item (w '-!' w item)*
		Span[] args=x.children();
		int i,j=0,n=args.length;
		if (n==1) return compile(args[0],host);
		int[] ops=new int[n];
		ops[j++]=compile(args[0],host);
		for (i=1;i<n;i++) {
			int op=compile(args[i],host);
			if (op<0) continue;
			int z=ops[j-1];
			if (isChs(z) && isChs(op)) ops[j-1]=excludeChs(z,op);
			else { ops[j-1]=opload(Pam.NOT,op); ops[j++]=z; } // !op z
		}
		if (j==1) return ops[0];
		return Pam.opcode(Pam.SEQ,codePad.load(ops,j));
	}

	int code(Span x, int host) { 
		// code = int int? | '0' ('x'/'X') hexs / ints
		Span[] args=x.children();
		int n=args.length;
		if (n==1) return compile(args[0],host);
		return chs(integer(args[0]),integer(args[1]));
	}

	int hexs(Span x, int host) { 
		// hexs = hex ('..' hex)?
		Span[] args=x.children();
		int n=args.length, k=hex(args[0]);
		if (n==1) return chs(k);
		return chs(k,hex(args[1]));
	}

	int quote(Span x, int host) { 
		// quote = quo quo? 
		Span[] args=x.children();
		int n=args.length;
		if (n==1) return quo(args[0]);
		Span q1=args[0],q2=args[1];
		if (q1.eot-q1.sot!=3 || q2.eot-q2.sot!=3)
			System.out.printf("bad quote: %s\n",text(x));
		return chs(intQuo(args[0]),intQuo(args[1]));
	}
	
	int quo(Span x) { 
		// quo :  '' 'x' 'xyz'
		String txt=text(x);
		int len=txt.length();
		if (len==2) return Pam.opcode(Pam.NOP,0); // '' empty string
		if (len==3) return chs(intQuo(x)); // 'x'
		int[] ops=new int[len-2]; // 'xyz'
		for (int i=1; i<len-1; i++) {
			int cc=txt.codePointAt(i);
			ops[i-1]=chs(cc); // CHS cc
		}
		return Pam.opcode(Pam.SEQ,codePad.load(ops));
	}

	int pat(Span x, int host) {
		// pat = eq? ref; ref=name ('.' name)*
		//   @name  -- PAT predicate prior name exists...
		//   @=name -- PEQ find prior name span and match same again
		Span[] args=x.last().children();
		int n=args.length;
		int[] tags=new int[n];
		int i=0;
		for (Span arg:args) tags[i++]=ruleTag(text(arg));
		int arg=codePad.load(tags);
		if (x.first().tag==parser.tag("eq")) return Pam.opcode(Pam.PEQ,codePad.load(tags));
		return Pam.opcode(Pam.PAT,codePad.load(tags));
	}

	
	// -- External grammar links -------------------------------------------------

	Map<String,Gist> glib; // external grammar library
	List<String> defaults; // glib names keys to search for undefined names
	Map<String,Integer> linkMap; // index of actual external grammars used
		
	int findDefault(Boolean elide, String name, int host) {
		if (glib==null) return -1;
		for (String label:defaults) {
			int ext=findExternal(elide,label,name,host);
			if (ext>=0) return ext;
		}
		return -3; // rule not found
	}
		
	int findExternal(Boolean elide, String label, String name, int host) {
		if (glib==null) return -1; // no library given
		Gist gist=glib.get(label);
		if (gist==null) return -2; // label-grammar undefined
		int tag=gist.parser.tag(name);
		if (tag<0) return -3; // rule not found in label grammar
		if (elide || isLeaf(host)) {
			int[] xcode=gist.parser.code();
			int body=xcode[tag];
			if (Pam.op(body)==Pam.CHS)
				return Pam.opcode(Pam.CHS,codePad.loadCopy(xcode,Pam.arg(body)));
			else return Pam.extRun(findLink(label),tag);
		} else return Pam.extRef(findLink(label),tag);		
	}

	int findLink(String label) {
		if (linkMap==null) linkMap=new HashMap<String,Integer>();
		linkMap.put("0",new Integer(0)); // avoid link==0
		Integer link=linkMap.get(label);
		if (link==null) { // first ref...
			link=new Integer(linkMap.size());
			linkMap.put(label,link);
		}
		return link.intValue();
	}
	
	Parser[] parserLinks() {
		if (linkMap==null) return null;
		Parser[] links=new Parser[linkMap.size()];
		for (Map.Entry<String,Integer> entry:linkMap.entrySet()) {
			int i=entry.getValue().intValue(); // label->link
			if (i>0) links[i] = glib.get(entry.getKey()).parser;
		}
		return links;
	}

	// -- CHS merge and exclude --------------------------------------------------
	
	int mergeChs(int x, int y) { // x|y
		int[] cs=codePad.code;
		int ax=Pam.arg(x), ay=Pam.arg(y);
		int nx=cs[ax], ny=cs[ay]; // sizes
		int x0=ax+1, y0=ay+1; // index first val
		int[] xs=new int[nx], ys=new int[ny];
		for (int i=0;i<nx;i++) xs[i]=cs[x0+i];
		for (int j=0;j<ny;j++) ys[j]=cs[y0+j];
		int[] zs=new int[nx+ny];
		int i=0,j=0,k=0; // index for xs,ys,zs
		while (i<nx && j<ny) {
			int a=xs[i], b=xs[i+1];
			int c=ys[j], d=ys[j+1];
			if (b+1<c) {zs[k++]=a; zs[k++]=b; i+=2;} // a..b  c..d
			else if (d+1<a) {zs[k++]=c; zs[k++]=d; j+=2;} // c..d  a..b
			else if (b>=d) {xs[i]=min(a,c); j+=2;} // overlap a..b higher
			else if (d>=b) {ys[j]=min(a,c); i+=2;} // overlap c..d higher
			else woops("what?");
		}
		k=absorb(i,xs,zs,k);
		k=absorb(j,ys,zs,k);
		return Pam.opcode(Pam.CHS,codePad.load(zs,k));
	}

	int absorb(int i, int[] xs, int[] zs, int k) {
		if (i>=xs.length) return k; // all done
		if (k==0) {zs[k++]=xs[i]; zs[k++]=xs[i+1]; absorb(i+2,xs,zs,k);}
		int p=zs[k-1], q=xs[i];
		if (p+1>=q) { // overlap....
			zs[k-2]=min(xs[i],zs[k-2]);
			zs[k-1]=max(xs[i+1],zs[k-1]);
			return absorb(i+2,xs,zs,k);
		} 
		while (i<xs.length) zs[k++]=xs[i++];
		return k;
	}

	int excludeChs(int x, int noty) { // x-!y
		int[] cs=codePad.code;
		int y; // chs to exclude
		if (isNotChs(noty)) y=cs[Pam.arg(noty)];
		else y=noty; // isChs(noty)
		int ax=Pam.arg(x), ay=Pam.arg(y);
		int nx=cs[ax], ny=cs[ay]; // sizes
		int x0=ax+1, y0=ay+1; // index first val
		int[] xs=new int[nx], ys=new int[ny];
		for (int i=0;i<nx;i++) xs[i]=cs[x0+i];
		for (int j=0;j<ny;j++) ys[j]=cs[y0+j];
		int[] zs=new int[nx+ny];
		int i=0,j=0,k=0; // index for xs,ys,zs
		while (i<nx) {
			int a=xs[i], b=xs[i+1];
			if (j<ny) {
				int c=ys[j], d=ys[j+1];
				if (b<c) j+=2; // x:a..b < c..d
				else if (d<a) j+=2; // c..d < x:a..b
				else if (c<=a && d>=b) {i+=2; j=0;} // !c..d excludes a..b
				else if (c<=a) {xs[i]=d+1; j+=2;} // d+1..b
				else if (d>=b) {xs[i+1]=c-1; j+=2;} // a..c-1
				else {zs[k++]=a; zs[k++]=c-1; xs[i]=d+1; j+=2;} // split....
			} else { zs[k++]=a; zs[k++]=b; i+=2; j=0; } // ok all excluded, move on...
		}
		return Pam.opcode(Pam.CHS,codePad.load(zs,k));
	}	

	// -- utility methods --------------------------------------------
	
	int min(int a, int b) {if (a<b) return a; else return b;}
	int max(int a, int b) {if (a>b) return a; else return b;}

	boolean isChs(int coda) {return (Pam.op(coda)==Pam.CHS);}
	
	boolean isNotChs(int coda) {return (Pam.op(coda)==Pam.NOT && isChs(codePad.code[Pam.arg(coda)]));} 
		
	int ruleBody(int rule) {return codePad.code[rule];}
	
	int chs(int... xs) {
		if (xs.length==1) return chs(xs[0],xs[0]);
		return Pam.opcode(Pam.CHS,codePad.load(xs));
	}
		
	int opload(int op,int arg) {
		return Pam.opcode(op,codePad.load(arg));
	}

	String text(Span x) { return gist.substring(x.sot,x.eot); }
	
	int integer(Span x) { return new Integer(gist.substring(x.sot,x.eot)).intValue(); }
	
	int hex(Span x) { return Integer.valueOf(gist.substring(x.sot,x.eot),16).intValue(); }
	
	int intQuo(Span x) { return gist.codePointAt(x.sot+1); }
	
	boolean isTag(String tag,Span x) { return tag.equals(parser.ruleNames[x.tag]); }

	boolean isText(String txt,Span x) { return gist.startsWith(txt,x.sot); }

}