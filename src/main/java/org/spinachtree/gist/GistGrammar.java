package org.spinachtree.gist;

import java.util.*;

abstract class Grammar {	
	abstract void buildParser(Parser par);
}

class GistGrammar extends Grammar {
	
	GistGrammar() { boot(); }
	
	GistGrammar(String grammar) {
		if (transform==null) boot();
		this.grammar=grammar;
	}

	GistGrammar(Term gist) {
		if (transform==null) boot();
		if (!gist.isTag("gist"))
			throw new GistFault("GistGrammar requires a gist parse tree:\n"+gist);
		this.gist=gist;
	}

	void boot() {
		transform=new Transform(this.getClass());
		Parser bootParser=new Parser(new Boot());
		Term rules=bootParser.parse(gistGrammar);
		gistParser=new Parser(new Boot(rules));
		gistParser=new Parser(new GistGrammar(gistGrammar));
	}
	
	static Transform transform;   // to walk parse tree and call Java methods
	static Parser gistParser;     // parser for the gistGrammar
	
	void buildParser(Parser par) {
		this.par=par; // the parser to contain the grammar
		if (grammar!=null) gist=gistParser.parse(grammar);
		if (gist==null) gist=gistParser.parse(gistGrammar);
		if (!gist.isTag("gist")) throw new GistFault("\n"+gist);
		try { compile(gist); }
		catch (Exception e) { throw new GistFault("\n"+e+"\n"+par.fault+"\n"+gist); }
		if (par.fault!=null) throw new GistFault("\n"+par.fault+"\n"+gist);
	}
	
	String grammar;       // source text rules
	Term gist;            // parse tree from gistGrammar
	Parser par;           // target parser, global var for compile methods
	
	// Full Gist language grammar, but expressed using Boot grammar language...
	// Boot does not accept: comments, !, @, hex 0xff, external refs, etc..

	public static final String gistGrammar = rules(
		"gist    = (xs (rule/import) s ';'?)* xs ",
		"rule    = name elide? xs defn xs sel ",
		"sel     = seq xs '/' xs sel / seq ", 
		"seq     = item s (',' xs)? seq / item ", 
		"item    = factor rep? / prime ",
		"rep     = '+'/'?'/'*' ints? ",
		"factor  = fact (s butnot s fact)* ",
		"fact    = ident/literal/group/prior/event ", 
		"prime   = option/many/not/peek ", 
		"group   = '(' exp ')' ", 
		"option  = '[' exp ']' ", 
		"many    = '{' exp '}' ",
		"exp     = (xs sel xs)* ",
		"not     = '!' s factor ", 
		"peek    = '&' s factor ", 
		"prior   = '@' s name ", 
		"event   = '<' s name? s args? '>' ", 
		"literal = quote / str / code ", 
		"quote   = quo ('..' quo)? ", 
		"code    = '0' ('x'/'X') hexs / ints ", 
		"ints    = int ('..' (int/anon))? ", 
		"hexs    = hex ('..' ('0'('x'/'X'))? hex)? ", 
		"ident   = ref / anon / skip / newln ",
		"ref     = path name elide? ",
		"import  = '_' s defn s label '._'? (s ','? s label '._'?)* ",
		"label   : name ('.' name)* ",
		"path    : (name '.')*  ",
		"name    : alpha (alnum/'_')* ",
		"defn    : '=' / ':' ", 
		"int     : digit+ ", 
		"hex     : (digit/'a'..'f'/'A'..'F')+ ", 
		"quo     : 39 (32..38/40..126) 39 ", 
		"str     : 39 (32..38/40..126)* 39 ", 
		"s       : blank* ", 
		"xs      : (sp* comment?)* ", 
		"comment : ('--' / '//' / '#') print* ",
		"args    : (32..61/63..126)+ ", // !'>'
		"alnum   : alpha/digit ", 
		"alpha   : 'a'..'z'/'A'..'Z' ", 
		"digit   : '0'..'9' ", 
		"elide   : '..' ", 
		"colon   : ':' ", 
		"butnot  : '-!' ", 
		"skip    : '~' ", 
		"newln   : '$' ", 
		"anon    : '_' ", 
		"blank   : 9/32 ", 
		"print   : 9/32..1114111 ",
		"sp      : 9..13/32 " );

	// Transform methods for gistGrammar rules ....................................
	
	Op sel(Op x, String xs1, String xs2, Op y) { return x.or(y); }
	Op seq(Op x, String s, Op y) { return x.and(y); }
	Op seq(Op x, String s, String xs, Op y) { return x.and(y); }
	Op item(Op x, String rep) {
		if (rep==null) return x;
		if (rep.equals("*")) return x.rep();
		if (rep.equals("+")) return x.rep1();
		if (rep.equals("?")) return x.opt();
		else return x;
	}
	Op factor(ArrayList<Op> ops) { return notList(1,ops.get(0),ops); }
	Op group(Op x) { if (x.And==null) return x; else return new Grp(par,x); }

	Op fact(Op x) { return x; }
	Op prime(Op x) { return x; }
	Op option(Op x) { return group(x).opt(); }
	Op many(Op x) { return group(x).rep(); }
	Op literal(Op x) { return x; }
	Op code(Op x) { return x; }
	Op ident(Op x) { return x; }
	
	Op exp(String _, ArrayList<Op> ops, String _x) { return andList(1,ops.get(0),ops); }

	Op not(String _s, Op x) { return new Not(par,x); }
	Op peek(String _s, Op x) { return new Peek(par,x); }
	Op prior(String _s, Op x) { return new Prior(par,x); }

	Op ref(String path, String name, String elide) { // path name elide?
		if (path.length()>0) return externalRef(path,name,elide);
		Rule rule=buildRule(name);
		Op op=inline(rule);
		if (op!=null) return op;
		return new Ref(par,host(),rule,(elide!=null));
	}

        Op skip(String _) { return new WhiteSpace(par); } // ~ => xs..*
        Op newln(String _) { return new NewLine(par); } // $ => XML1.1 eol
        Op anon(String _) { return new Chs(par,new int[] {0,0x10FFFF},1); } // _ => any

	Op event(String _s, String name,String _s1, String args) { return new Event(par,host(),name,args); }

	Op ints(String min, String max) { return new Chs(par,min,max); }
	Op hexs(String min, String max) { return new Chs(par,min,max,16); }
	Op quote(String q1, String q2) { return new Chs(par,q1,q2); }
	Op str(String s) { return new Str(par,s); }

	Op andList(int i, Op x, ArrayList<Op> ops) {
		if (i>ops.size()-1) return x;
		return andList(i+1,x.and(ops.get(i)),ops);
	}

	Op notList(int i, Op x, ArrayList<Op> ops) {
		if (i>ops.size()-1) return x;
		Op y=ops.get(i);
		if (x.except(y)) return notList(i+1,x,ops);
		return new Not(par,y).and(notList(i+1,x,ops));
	}

	// Compile parse tree => parser Ops ---------------------------------------------------

	Map<String,Term> termMap=new HashMap<String,Term>();

	void compile(Term gist) {
		List<String> ruleNames=mapRuleTerms(gist);
		if (ruleNames.isEmpty())
			throw new GistFault("\nNo 'rule' terms found in parse tree...");
		buildRules();
		par.putRule("=",new Rules(par,ruleNames));
	}
	
	List<String> mapRuleTerms(Term term) {
		List<String> ruleNames=new ArrayList<String>();
		for (Term t:term) {
			if (t.isTag("rule")) {
				String name=t.text("name");
				ruleNames.add(name);
				Term pre=termMap.get(name);
				if (pre==null) termMap.put(name,t);
				else par.fault("Duplicate rule: "+name);
			} else if (t.isTag("import")) importRule(t);
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
		if (term==null) {
			
			term=par.faultTerm("Missing rule: "+name);
		}
		rule=new Rule(par,name,term.has("elide"),term.has("defn",":"),null);
		par.putRule(name,rule); // target for refs during build...
		try { rule.body=(Op)transform.build(this,term.child("sel")); }
		catch (Exception e) { throw new GistFault("buildRule "+buildStack+" "+e); }
		buildStack.pop();
		return rule;
	}

	String hostName() { return buildStack.peek(); }	
	Rule host() { return par.getRule(hostName()); }

	Op inline(Rule rule) { // macro expansion copy...
		if (host().term && rule!=null && rule.body!=null) {
			Op bod=rule.body.copy();
			if (bod!=null) return bod;
		}
		return null;
	}
	
	// External rules----------------------------------------------------------------------
	
	List<Parser> imports=new ArrayList<Parser>();
	static Map<String,Parser> library=new HashMap<String,Parser>();

	void importRule(Term imp) {
		// import  = '_' s defn s label '._'? (s ','? s label '._'?)*
		for (Term label : imp)
			if (label.isTag("label")) {
				String grammar=label.text();
				Parser parser=getParser(grammar);
				if (parser==null) faultGrammar(grammar);
				else imports.add(parser);
			}
	}

	Parser getParser(String grammar) {
		Parser parser=library.get(grammar);
		if (parser==null) {
			String rules=Library.get(grammar);
			if (rules!=null) parser=new Parser(new GistGrammar(rules));
			library.put(grammar,parser);
		}
		return parser;
	}
	
	Rule importRule(String name) {
		for (Parser parser:imports) {
			Rule rule=parser.getRule(name);
			if (rule!=null) return rule;
		}
		return null;	
	}

	Op externalRef(String path, String name, String elide) {
		// ref = path name elide? 
		String grammar=path.substring(0,path.length()-1); // trim final '.'
		Parser parser=getParser(grammar);
		if (parser==null) return faultGrammar(grammar);
		Rule rule=parser.getRule(name);
		if (rule==null) {
			par.fault("Undefined external rule: "+path+rule);
			return new False(par);
		}
		return new Ref(par,host(),rule,(elide!=null));
	}

	Op faultGrammar(String grammar) {
		par.fault("Unknown grammar: "+grammar+" Use: Gist.load(\""+grammar+"\",rules...) ");
		return new False(par);
	}
	
	static String rules(String... lines) {
		StringBuffer sb=new StringBuffer();
		for (String line: lines) sb.append(line).append("\n");
		return sb.toString();
	}

} // Grammar

