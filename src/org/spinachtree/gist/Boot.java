
package org.spinachtree.gist;

// PBNF Boot
//  ==================================================

class Boot {
	
	Boot() {
		Parser boot=new Parser(rules);
	}

	Parser boot;

	// Syntax: the grammar for PBNF boot rules....

	static final String bootGrammar =
	"	pbnf   = (rule | `s)*				\n"+
	"	rule   = name `s defn `s sel			\n"+
	"	sel    = seq (`bar seq)*			\n"+
	"	seq    = rep (`step rep)*			\n"+
	"	rep    = elem sufx?				\n"+
	"	elem   = ref | quots | code | group		\n"+
	"       group  = '(' `s sel `s ')'			\n"+
	"	quots  = quo ('.' '.' quo)?			\n"+
	"	code   = int ('.' '.' int)?			\n"+
	"	ref    = elide? name				\n"+
	"	quo    : 39 (32..38|40..126)* 39		\n"+
	"	int    : '0'..'9'+				\n"+
	"	name   : ('A'..'Z'|'a'..'z')+			\n"+
	"	defn   : '=' | ':'				\n"+
	"	sufx   : '*' | '+' | '?'			\n"+
	"	elide  : '`'					\n"+
	"	bar    : s '|' s				\n"+
	"	step   : ' '* (',' s)?				\n"+
	"	s      : (9 | 10 | 13 | 32)*    		\n";

	// Semantics: translate PBNF boot rules into grammar Op expressions.......
	
	public Object[] pbnf(Object[] rules) {
		return rules;
	}
	
	public Op rule(Object[] xs) {
		return new Op_rule((String)xs[0],(String)xs[1],(Op)xs[2]);
	}

	public Op sel(Object[] seqs) {
		if (seqs.length==1) return (Op)(seqs[0]);
		return new Op_sel(seqs);
	}

	public Op seq(Object[] reps) {
		if (reps.length==1) return (Op)reps[0];
		return new Op_seq(reps);
	}
	
	public Op rep(Object[] xs) {
		Op elem=(Op)(xs[0]);
		if (xs.length==1) return elem;
		String sufx=(String)xs[1];
		if (sufx.equals("*")) return new Op_rep(elem);
		if (sufx.equals("+")) return new Op_rep1(elem);
		if (sufx.equals("?")) return new Op_opt(elem);
		fault("unknown sufx: "+sufx);
		return null;
	}
	
	public Op ref(Object[] xs) { // elide? name
		if (xs.length==1) return new Op_call((String)xs[0]);
		return new Op_call((String)xs[1],true);
	}
	
	public Op quots(Object[] xs) { // 'x' | 'x'..'z'
		if (xs.length==1) {
			String qs=(String)xs[0];
			int len=qs.length();
			if (len==2) return new Op_empty();
			if (len==3) return new Op_chs(q1(qs));
			Op[] cseq=new Op[len-2];
			for (int i=1;i<len-1;i++) cseq[i-1]=new Op_chs(qs.codePointAt(i));
			return new Op_seq(cseq);
		}
		return new Op_chs(new int[] {q1(xs[0]),q1(xs[1])});
	}

	int q1(Object quot) { return ((String)quot).codePointAt(1); } // 'x'
	
	public Op code(Object[] xs) { // 123 | 123..456
		if (xs.length==1) return new Op_chs(new Integer((String)xs[0]));
		return new Op_chs(new int[] {new Integer((String)xs[0]),new Integer((String)xs[1])});
	}
	
	void fault(String msg) {
		System.out.println(msg);
	}

	// hand compile PBNF boot rules into Op terms.......................................................................

	Op_rule[] rules=new Op_rule[] {
		rule("pbnf", "=",  rep(sel(call("rule"),run("s")))),
		rule("rule", "=",  seq(call("name"),run("s"),call("defn"),run("s"),call("sel"))),
		rule("sel",  "=",  seq(call("seq"),rep(seq(run("bar"),call("seq"))))),
		rule("seq",  "=",  seq(call("rep"),rep(seq(run("step"),call("rep"))))),
		rule("rep",  "=",  seq(call("elem"),opt(call("sufx")))),
		rule("elem", "=",  sel(call("ref"),call("quots"),call("code"),call("group"))),
		rule("group","=",  seq(chs("("),run("s"),call("sel"),run("s"),chs(")"))),
		rule("quots","=",  seq(call("quo"),opt(seq(chs("."),chs("."),call("quo"))))),
		rule("code", "=",  seq(call("int"),opt(seq(chs("."),chs("."),call("int"))))),
		rule("ref",  "=",  seq(opt(call("elide")),call("name"))),
		rule("quo",  ":",  seq(chs(39),rep(sel(chs(32,38),chs(40,126))),chs(39))),
		rule("int",  ":",  rep1(chs(48,57))),
		rule("name", ":",  rep1(chs(65,90,97,122))),
		rule("defn", ":",  chs(":","=")),
		rule("sufx", ":",  chs("*","+","?")),
		rule("elide",":",  chs("`")),
		rule("bar",  ":",  seq(run("s"),chs("|"),run("s"))),
		rule("step", ":",  seq(rep(chs(32)),opt(seq(chs(","),run("s"))))),
		rule("s",    ":",  rep(chs(9,10,13,13,32,32)))
	};

	Op_rule rule(String rule, String defn, Op body) { return new Op_rule(rule,defn,body); }
	Op_call run(String rule) { return new Op_call(rule,true); }
	Op_call call(String rule) { return new Op_call(rule); }
	Op_seq seq(Op...ops) { return new Op_seq(ops); }
	Op_sel sel(Op...ops) { return new Op_sel(ops); }
	Op_rep rep(Op op) { return new Op_rep(op); }
	Op_rep1 rep1(Op op) { return new Op_rep1(op); }
	Op_opt opt(Op op) { return new Op_opt(op); }
	Op_chs chs(String...cs) { return new Op_chs(cs); }
	Op_chs chs(int...rs) { return new Op_chs(rs); }
	
	// testing.....................................

	public static void main(String[] args) {
		Boot boot=new Boot();
		
		System.out.printf("Boot.......%n");

		Op_rule[] rules=boot.rules;
		
		Parser parser=new Parser(rules);
		System.out.println(parser); // print the op rules
		
		String input=boot.bootGrammar; // start with a simpler input before testing this
		Span parse=parser.parse(input);
		System.out.printf("input length=%d, parse length=%d max=%d %n",input.length(),parse.eot,0);
		
		System.out.println("Run time......boot-boot");
		int num=1000;
		long start=System.nanoTime();
		Span p;
		for (int i=0;i<num;i++) p=parser.parse(input);
		long end=System.nanoTime();
		long tt=end-start;
		System.out.println("Run time for "+num+" = "+(tt/1000000)+" ms, = "+(tt/num)+" ns/parse");

		System.out.printf("transform....................... %n");
		Transform transform=new Transform(boot,parser);
		Object[] ruleops=(Object[])transform.transform(input,parse);
		Parser psr=new Parser(ruleops);
		System.out.println(psr); // print the op rules
		
		// then try Gist to tie it all together....................
		
		Gist bootGist=new Gist(boot.bootGrammar,boot);
		runTime("boot-boot",bootGist,boot.bootGrammar);
		runTime("boot-boot",bootGist,boot.bootGrammar);

	}

	static void runTime(String label,Gist gist,String text) {
		System.out.println("Run time......gist: "+label);
		int num=1000;
		long start=System.nanoTime();
		Object result;
		for (int i=0;i<num;i++) result=gist.transform(text);
		long end=System.nanoTime();
		long tt=end-start;
		System.out.println("Run time for "+num+" = "+(tt/1000000)+" ms, = "+(tt/num)+" ns/parse");
	}


} // Boot



