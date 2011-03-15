
package org.spinachtree.gist;

// this is the first test driver for initial development....

class Test {
	
	final String calcGrammar = 
		"	arith = term (add term)*		\n"+
		"	term  = val (mult val)*			\n"+
		"	val   = num | '(' arith ')'		\n"+
		"	add   : '+' | '-'			\n"+
		"	mult  : '*' | '/'			\n"+
		"	num   : '0'..'9'+			";
		
	// --- transform method for calcGrammar -----------------------------
	
	public Integer arith(Object[] args) {
		if (args.length==1) return (Integer)args[0];
		return reduce((Integer)args[0],1,args);
	}
	
	public Integer term(Object[] args) {
		return arith(args);
	}
	
	public Integer val(Object[] args) {
		Object x=args[0];
		if (x instanceof String) return new Integer((String)x);
		return (Integer)x;
	}

	// --- end transform method for calcGrammar -----------------------------

	Integer reduce(int x, int i, Object[] args) {
		if (i>=args.length) return x;
		String op = (String)args[i];
		int y = (Integer)args[i+1];
		return reduce(calc(x,op,y),i+2,args);
	}
		
	int calc(int x, String op, int y) {
		if (op.equals("+")) return (x+y);
		if (op.equals("-")) return (x-y);
		if (op.equals("*")) return (x*y);
		if (op.equals("/")) return (x/y);
		return 0; // should throw exception
	}
		
	// hand compile PBNF into Op rules........	

	Op_rule[] rules=new Op_rule[] {
		rule("arith", "=", seq(call("term"),rep(seq(call("add"),call("term"))))),
		rule("term",  "=", seq(call("val"),rep(seq(call("mult"),call("val"))))),
		rule("val",   "=", sel(call("num"),seq(chs("("),call("arith"),chs(")")))),
		rule("add",   ":", chs("+","-")),
		rule("mult",  ":", chs("*","/")),
		rule("num",   ":", rep1(chs(48,57)))
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
	
	
	public static void main(String[] args) {
		Test test=new Test();

		// -- first test Parser and the Op grammar operators.....

		Op_rule[] rules=test.rules;
		
		Parser parser=new Parser(rules);
		System.out.println(parser); // print the op rules
		
		String input="1+2*3";
		Span parse=parser.parse(input);
		System.out.printf("input length=%d, parse length=%d %n",input.length(),parse.eot);
		
		// -- next, test a transform....
		
		Transform transform=new Transform(test,parser);

		System.out.printf("transform....................... %n");
		Integer result=(Integer)transform.transform(input,parse);
		System.out.printf("%s => %d %n",input,result);
	}
	
}