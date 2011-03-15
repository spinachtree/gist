package org.spinachtree.gist;

// PBNF Grammar Grammar
//  ==================================================

class PBNF {
	
	// Gist PBNF grammar expressed as a boot grammmar:
	// - rule names using letters only
	// - no !x or &x or @x predicates
	// - no hex char codes 0x20
	// - no {many} or [optional] syntax
	// - no x^y syntax
	// - no comments

	static final String pbnfGrammar = 
	"pbnf    = (rule | `w)*	    				\n"+
	"rule    = name `w defn `w sel				\n"+
	"sel     = alt (`w '|' `w alt)*				\n"+
	"alt     = seq (`w '/' `w seq)*				\n"+
	"seq     = rep (`h (',' `w)? rep)*			\n"+
	"rep     = elem repn? | prime				\n"+
	"elem    = item (`w '^' `w item)*			\n"+
	"item    = ref|quots|code|group     			\n"+
	"prime   = many|option|not|isa|pre  			\n"+
	"group   = '(' `w sel `w ')'				\n"+
	"many    = '{' `w sel `w '}'				\n"+
	"option  = '[' `w sel `w ']'				\n"+
	"not     = '!' `h rep					\n"+
	"isa     = '&' `h rep					\n"+ 
	"pre     = '@' eq? `h name ('.' name)*			\n"+
	"quots   = quo ('..' quo)?				\n"+ 
	"code    = val ('..' val)?				\n"+
	"val     = int | hx hex					\n"+
	"ref     = elide? name ('.' name)? 			\n"+
	"name    : alpha (alnum|'_')*				\n"+
	"defn    : '=' | ':'					\n"+
	"repn    : '+'|'?'|'*'					\n"+
	"int     : digit+					\n"+
	"hx      : '0' ('x'|'X')				\n"+
	"hex     : (digit|'a'..'f'|'A'..'F')+			\n"+
	"quo     : 39 (32..38|40..126)* 39			\n"+
	"alnum   : alpha|digit					\n"+
	"alpha   : 'a'..'z'|'A'..'Z'				\n"+
	"digit   : '0'..'9'					\n"+
	"eq      : '='						\n"+
	"elide   : '`'						\n"+
	"blank   : 9|32						\n"+
	"print   : 9|32..1114111				\n"+
	"space   : 9..13|32					\n"+
	"comment : ('--'|'//'|'#') print*			\n"+
	"h       : blank*					\n"+ 
	"s       : space*					\n"+ 
	"w       : (s comment?)*				\n"; 

	// Semantics: translate PBNF rules into grammar Op expressions.......

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

	public Op alt(Object[] seqs) {
		if (seqs.length==1) return (Op)(seqs[0]);
		return new Op_alt(seqs);
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
	
	public Op elem(Object[] xs) { // x(^y)* => !y.. x
		Object x=xs[0];
		if (xs.length==1) return (Op)x;
		for (int i=1;i<xs.length;i++) xs[i-1]=new Op_not((Op)xs[i]);
		xs[xs.length-1]=x;
		return new Op_seq(xs);
	}
	
	public Op many(Object[] xs) { // { x }
		return new Op_rep((Op)xs[0]);
	}
	
	public Op option(Object[] xs) { // [ x ]
		return new Op_opt((Op)xs[0]);
	}
	
	public Op not(Object[] xs) { // !x
		return new Op_not((Op)xs[0]);
	}
	
	public Op isa(Object[] xs) { // &x
		return new Op_isa((Op)xs[0]);
	}
	
	public Op pre(Object[] xs) { // @x | @=x => '@' eq? `h ref
		if (((String)xs[0]).equals("=")) return new Op_peq(xs);
		return new Op_pre(xs);
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
	
	public Op code(Object[] xs) { // val ('..' val)?
		if (xs.length==1) return new Op_chs((Integer)xs[0]);
		return new Op_chs(new int[] {(Integer)xs[0],(Integer)xs[1]});
	}

	public Integer val(Object[] xs) { // int | hx hex
		if (xs.length==1) return new Integer((String)xs[0]);
		return Integer.valueOf((String)xs[1],16);
	}


	void fault(String msg) {
		System.out.println(msg);
	}

	// --- testing ----------------------------------------------------------------------------------------
	
	public static void main(String[] args) {
		PBNF pbnf=new PBNF();
		
		System.out.printf("PBNF ...boot.......%n");

		Gist bootGist=new Gist(Boot.bootGrammar,pbnf);
		//System.out.println(bootGist.ruleCode());
		
		System.out.printf("PBNF ...pbnf.......%n");

		Gist pbnfGist=new Gist(pbnf.pbnfGrammar,pbnf);
		//System.out.println(pbnfGist.ruleCode());
		
		System.out.printf("PBNF ...pbnf transform boot.......%n");

		Object[] boot_rules=(Object[])pbnfGist.transform(Boot.bootGrammar);
		//System.out.println(new Parser(boot_rules));
		
		System.out.printf("PBNF ...pbnf transform pbnf.......%n");

		Object[] pbnf_rules=(Object[])pbnfGist.transform(pbnf.pbnfGrammar);
		//System.out.println(new Parser(pbnf_rules));
		
		Gist bootbootGist=new Gist(Boot.bootGrammar,new Boot());
		runTime("boot-boot",bootbootGist,Boot.bootGrammar);
		runTime("boot-boot",bootbootGist,Boot.bootGrammar);
		
		Gist pbnfbootGist=new Gist(pbnfGrammar,new Boot());
		runTime("pbnf-boot",pbnfbootGist,Boot.bootGrammar);
		runTime("pbnf-boot",pbnfbootGist,Boot.bootGrammar);
		
		runTime("pbnf-pbnf",pbnfbootGist,pbnf.pbnfGrammar);
		runTime("pbnf-pbnf",pbnfbootGist,pbnf.pbnfGrammar);

	}

	static void runTime(String label,Gist gist,String text) {
		System.out.println("Gist run time.........: "+label);
		int num=1000;
		long start=System.nanoTime();
		Object result;
		for (int i=0;i<num;i++) result=gist.transform(text);
		long end=System.nanoTime();
		long tt=end-start;
		System.out.println("Run time for "+num+" = "+(tt/1000000)+" ms, = "+(tt/num)+" ns/parse");
	}

} // PBNF
