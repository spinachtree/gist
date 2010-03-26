package org.spinachtree.gist;

import java.util.*;

class Boot extends Grammar {
	
	Boot() { tree=null; }

	Boot(Term tree) { 
		transform=new Transform(this.getClass());
		if (!tree.isTag("rules"))
			throw new GistFault("Boot requires a bootRules parse tree:\n"+tree);
		this.tree=tree;
	}
	
	Transform transform;
	Term tree;
	
	void buildParser(Parser par) {
		this.par=par; // parser to contain this boot grammar
		if (tree==null) bootstrap();
		else compile(tree);
	}

	static final String bootRules =
		"rules = (s rule)* s                 \n"+
		"rule  = Name s DEF s sel            \n"+
		"sel   = seq OR sel / seq            \n"+
		"seq   = item AND seq / item         \n"+
		"item  = fact REP?                   \n"+
		"fact  = ref/code/chr/str/group      \n"+
		"code  = Int ('..' Int)?             \n"+
		"chr   = quo ('..' quo)?             \n"+
		"group = '(' s sel s ')'             \n"+
		"ref   = Name                        \n"+
		"quo   : 39 (32..38/40..126) 39      \n"+
		"str   : 39 (32..38/40..126)* 39     \n"+
		"Name  : ('a'..'z'/'A'..'Z')+        \n"+
		"Int   : '0'..'9'+                   \n"+
		"REP   : '*' / '+' / '?'             \n"+
		"DEF   : '=' / ':'                   \n"+
		"OR    : s '/' s                     \n"+
		"AND   : (9/32)*                     \n"+
		"s     : 9..32*                      ";

	void bootstrap() { // hand compiled parser ops for bootRules.........
		rule("rules",and(rep(grp(and("s",re("rule")))),re("s")));
		rule("rule",and("Name",and("s",and("DEF",and("s",re("sel"))))));
		rule("sel",or(and("seq",and("OR",re("sel"))),re("seq")));
		rule("seq",or(and("item",and("AND",re("seq"))),re("item")));
		rule("item",and("fact",opt(re("REP"))));
		rule("fact",or("ref",or("code",or("chr",or("str",re("group"))))));
		rule("code",and("Int",opt(grp(and(st(".."),re("Int"))))));
		rule("chr",and("quo",opt(grp(and(st(".."),re("quo"))))));
		rule("group",and(ch("("),and("s",and("sel",and("s",ch(")"))))));
		rule("ref",re("Name"));
		term("quo",and(cc(39),and(grp(or(cc(32,38),cc(40,126))),cc(39))));
		term("str",and(cc(39),and(rep(grp(or(cc(32,38),cc(40,126)))),cc(39))));
		term("Name",rep1(grp(or(ch("a","z"),ch("A","Z")))));
		term("Int",rep1(ch("0","9")));
		term("REP",or(ch("*"),or(ch("+"),ch("?"))));
		term("DEF",or(ch("="),ch(":")));
		term("OR",and("s",and(ch("/"),re("s"))));
		term("AND",rep(or(cc(9),cc(32))));
		term("s",rep(cc(9,32)));
		par.putRule("=",new Rules(par,names));
	}

	Parser par; // target parser for boot rules

	void rule(String name,Op body) { putRule(name,false,false,body); }
	void term(String name,Op body) { putRule(name,false,true,body); }
	
	Op or(Op x, Op y) { return x.or(y); }
	Op or(String name, Op y) { return re(name).or(y); }
	Op and(Op x, Op y) { return x.and(y); }
	Op and(String name, Op y) { return re(name).and(y); }
	Op rep(Op x) { return x.rep(); }
	Op rep1(Op x) { return x.and(x.copy().rep()); }
	Op opt(Op x) { return x.or(TRUE()); }
	Op grp(Op x) { return new Grp(par,x); }
	Op re(String name) { return new Ref(par,name); }
	Op ch(String c1,String c2) { return new Chs(par,"'"+c1+"'","'"+c2+"'"); }
	Op ch(String c) { return new Chs(par,"'"+c+"'",null); }
	Op st(String s) { return new Str(par,"'"+s+"'"); }
	Op cc(int min,int max) { return new Chs(par,String.valueOf(min),String.valueOf(max)); }
	Op cc(int min) { return new Chs(par,String.valueOf(min),null); }
	Op TRUE() { return new True(par); }

	List<String> names = new ArrayList<String>();

	void putRule(String name, boolean elide, boolean term, Op body) {
		names.add(name);
		par.putRule(name,new Rule(par,name,elide,term,body));
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
	Op ref(String name) { 
		Rule rule=buildRule(name);
		Op op=inline(rule);
		if (op!=null) return op;
		return new Ref(par,name);
	}
	Op group(String s1, Op sel, String s2) { return new Grp(par,sel); }
	Op code(String min, String max) { return new Chs(par,min,max); }
	Op chr(String q1, String q2) { return new Chs(par,q1,q2); }
	Op str(String s) { return new Str(par,s); }
	String Int(String s) { return s; }

	// compile parse tree => parser Ops in Parser par................................
	
	Map<String,Term> termMap=new HashMap<String,Term>();

	void compile(Term rules) {
		List<String> ruleNames = mapTerms(rules);
		if (ruleNames.isEmpty())
			throw new GistFault("\nNo 'rule' terms found...");
		buildRules();
		par.putRule("=",new Rules(par,ruleNames));
		if (par.fault!=null)
			throw new GistFault("\n"+par.fault.toString());
	}
	
	List<String> mapTerms(Term term) {
		List<String> ruleNames=new ArrayList<String>();
		for (Term t:term) {
			if (t.isTag("rule")) {
				String name=t.text("Name");
				ruleNames.add(name);
				Term pre=termMap.get(name);
				if (pre==null) termMap.put(name,t);
				else par.fault("Duplicate rule: "+name);
			}
		}
		return ruleNames;
	}

	void buildRules() {
		for (String name:termMap.keySet()) {
			Rule rule=par.getRule(name);
			if (rule==null) buildRule(name);
		}
	}

	Stack<String> buildStack=new Stack<String>();

	Rule buildRule(String name) {
		Rule rule=par.getRule(name);
		if (rule!=null) return rule; // already built (or being built)
		buildStack.push(name);
		Term term=termMap.get(name);
		if (term==null) term=par.faultTerm("Missing rule: "+name);
		rule=new Rule(par,name,term.has("elide"),term.has("DEF",":"),null);
		par.putRule(name,rule); // target for refs during build...
		try { rule.body=(Op)transform.build(this,term.child("sel")); }
		catch (Exception e) { par.faultTerm(e.toString()); }
		buildStack.pop();
		return rule;
	}

	String hostName() { if (buildStack.isEmpty()) return null; else return buildStack.peek(); }

	Op inline(Rule rule) { // macro expansion copy...
		Rule host=par.getRule(hostName());
		if (host.term && rule.body!=null) {
			Op bod=rule.body.copy();
			if (bod!=null) return bod;
		}
		return null;
	}


} // Boot
