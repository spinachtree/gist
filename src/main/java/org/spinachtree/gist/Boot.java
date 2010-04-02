package org.spinachtree.gist;

import java.util.*;

class Boot {
	
	static Rules rules() { return (new Boot().rules); }
	static Rules rules(Term tree) { return (new Boot(tree).rules); }
	
	Boot() { bootstrap(); }

	Boot(Term tree) { 
		Transform transform=new Transform(this.getClass());
		compile=new Compile(transform,rules);
		compile.rules(this,tree);
	}
	
	Rules rules=new Rules();
	Compile compile;
		
	static final String bootRules =
		"rules = (s rule)* s                 \n"+
		"rule  = name s defn s sel            \n"+
		"sel   = seq OR sel / seq            \n"+
		"seq   = item AND seq / item         \n"+
		"item  = fact REP?                   \n"+
		"fact  = ref/code/chr/str/group      \n"+
		"code  = int ('..' int)?             \n"+
		"chr   = quo ('..' quo)?             \n"+
		"group = '(' s sel s ')'             \n"+
		"ref   = name                        \n"+
		"quo   : 39 (32..38/40..126) 39      \n"+
		"str   : 39 (32..38/40..126)* 39     \n"+
		"name  : ('a'..'z'/'A'..'Z')+        \n"+
		"int   : '0'..'9'+                   \n"+
		"REP   : '*' / '+' / '?'             \n"+
		"defn  : '=' / ':'                   \n"+
		"OR    : s '/' s                     \n"+
		"AND   : (9/32)*                     \n"+
		"s     : 9..32*                      ";

	void bootstrap() { // hand compiled parser ops for bootRules.........
		rule("rules",and(rep(grp(and("s",re("rule")))),re("s")));
		rule("rule",and("name",and("s",and("defn",and("s",re("sel"))))));
		rule("sel",or(and("seq",and("OR",re("sel"))),re("seq")));
		rule("seq",or(and("item",and("AND",re("seq"))),re("item")));
		rule("item",and("fact",opt(re("REP"))));
		rule("fact",or("ref",or("code",or("chr",or("str",re("group"))))));
		rule("code",and("int",opt(grp(and(st(".."),re("int"))))));
		rule("chr",and("quo",opt(grp(and(st(".."),re("quo"))))));
		rule("group",and(ch("("),and("s",and("sel",and("s",ch(")"))))));
		rule("ref",re("name"));
		term("quo",and(cc(39),and(grp(or(cc(32,38),cc(40,126))),cc(39))));
		term("str",and(cc(39),and(rep(grp(or(cc(32,38),cc(40,126)))),cc(39))));
		term("name",rep1(grp(or(ch("a","z"),ch("A","Z")))));
		term("int",rep1(ch("0","9")));
		term("REP",or(ch("*"),or(ch("+"),ch("?"))));
		term("defn",or(ch("="),ch(":")));
		term("OR",and("s",and(ch("/"),re("s"))));
		term("AND",rep(or(cc(9),cc(32))));
		term("s",rep(cc(9,32)));
	}

	void rule(String name,Op body) { putRule(name,false,false,body); }
	void term(String name,Op body) { putRule(name,false,true,body); }
	
	Op or(Op x, Op y) { return x.or(y); }
	Op or(String name, Op y) { return re(name).or(y); }
	Op and(Op x, Op y) { return x.and(y); }
	Op and(String name, Op y) { return re(name).and(y); }
	Op rep(Op x) { return x.rep(); }
	Op rep1(Op x) { return x.and(x.copy().rep()); }
	Op opt(Op x) { return x.or(TRUE()); }
	Op grp(Op x) { return new Grp(x); }
	Op re(String name) { return new Ref(rules,name); }
	Op ch(String c1,String c2) { return new Chs("'"+c1+"'","'"+c2+"'"); }
	Op ch(String c) { return new Chs("'"+c+"'",null); }
	Op st(String s) { return new Str("'"+s+"'"); }
	Op cc(int min,int max) { return new Chs(String.valueOf(min),String.valueOf(max)); }
	Op cc(int min) { return new Chs(String.valueOf(min),null); }
	Op TRUE() { return new True(); }

	void putRule(String name, boolean elide, boolean term, Op body) {
		rules.add(name);
		Rule rule=new Rule(name,elide,term,body);
		if (!term) rule.fixed=false;
		rules.putRule(name,rule);
	}

	// Transform rules for compiler:  parse tree => parser Ops ===========================

	Op sel(Op x, String or, Op y) { return x.or(y); }
	Op seq(Op x, String and, Op y) { return x.and(y); }
	Op item(Op x, String rep) {
		if (rep==null) return x;
		if (rep.equals("*")) return x.rep();
		if (rep.equals("+")) return x.and(x.copy().rep());
		if (rep.equals("?")) return x.opt();
		else return x;
	}
	Op fact(Op x) { return x; }
	Op ref(String name) { return compile.ruleRef(name); }

	Op group(String s1, Op sel, String s2) { return new Grp(sel); }
	Op code(String min, String max) { return new Chs(min,max); }
	Op chr(String q1, String q2) { return new Chs(q1,q2); }
	Op str(String s) { return new Str(s); }

	public String toString() {
		return rules.toString();
	}

} // Boot
