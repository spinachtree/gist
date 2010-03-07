
package org.spinachtree.gist;

import java.util.*;


class Parser {
	
	// Full Gist language grammar, but expressed using Boot grammar language...
	// Boot does not accept: comments, !, @, multi-line rules, external refs, ..

	public static final String gistGrammar = rules(
		"Gist    = (xs (Rule/Import) s ';'?)* xs ",
		"Rule    = name dots? xs defn xs Sel ",
		"Sel     = Seq (xs '/' xs Seq)* ", 
		"Seq     = Item (s (',' xs)? Item)* ", 
		"Item    = Factor Rep? / Prime ",
		"Rep     = '+'/'?'/'*' Ints? ",
		"Factor  = Fact (minus Fact)* ",
		"Fact    = Ref/Literal/Group/Prior/Event ", 
		"Prime   = Option/Many/Not/Peek ", 
		"Group   = '(' Exp ')' ", 
		"Option  = '[' Exp ']' ", 
		"Many    = '{' Exp '}' ",
		"Exp     = (Sel xs)* ",
		"Not     = '!' s Factor ", 
		"Peek    = '&' s Factor ", 
		"Prior   = '@' s name ", 
		"Event   = '<' name? s args? '>' ", 
		"Literal = Quote / Code ", 
		"Quote   = quote ('..' quote)? ", 
		"Code    = '0' ('x'/'X') Hexs / Ints ", 
		"Ints    = int ('..' (int/anon))? ", 
		"Hexs    = hex ('..' ('0'('x'/'X'))? hex)? ", 
		"Ref     = name ('.' name)* dots? / anon / skip / break ",
		"Import  = '_' s defn s label '._'? (s ','? s label '._'?)* ",
		"label   : name ('.' name)* ",
		"name    : alpha (alnum/'_')* ",
		"defn    : '=' / ':' ", 
		"int     : digit+ ", 
		"hex     : (digit/'a'..'f'/'A'..'F')+ ", 
		"quote   : 39 (32..38/40..126)* 39 ", 
		"s       : blank* ", 
		"xs      : (sp* ';'? comment?)* ", 
		"comment : ('--' / '//' / '#') print* ",
		"args    : (32..61/63..126)+ ", // !'>'
		"alnum   : alpha/digit ", 
		"alpha   : 'a'..'z'/'A'..'Z' ", 
		"digit   : '0'..'9' ", 
		"dot     : '.' ", 
		"dots    : '..' ", 
		"colon   : ':' ", 
		"minus   : '-' ", 
		"skip    : '~' ", 
		"break   : '$' ", 
		"anon    : '_' ", 
		"blank   : 9/32 ", 
		"print   : 9/32..126 ", 
		"sp      : 9..13/32 " );

	static String rules(String... lines) {
		StringBuffer sb=new StringBuffer();
		for (String line: lines) sb.append(line).append("\n");
		return sb.toString();
	}

	static Parser gistParser = new Parser(new Boot().parse(gistGrammar));

	static Map<String,Parser> library=new HashMap<String,Parser>();
	
	Parser(String rules) {
		this(gistParser.parse(rules));
	}

	Parser(Term gist) {
		if (!gist.isTag("Gist"))
			throw new GistFault("\n"+gist.toString());
		compile(gist);
		if (fault!=null)
			throw new GistFault("\n"+fault.toString());
	}
	
	Term parse(String src) {
		Scan scan=new Scan(src,0);
		if (action!=null) scan.action=action;
		boolean result=ruleMap.get(start).parse(scan);
		if (!result) return scan.faultResult(start+" parse failed... "); 
		if (scan.pos<scan.eot) return scan.faultResult(start+" parse incomplete... "); 
		return scan.root();
	}

	List<Parser> imports=new ArrayList<Parser>();

	HashMap<String,Term> termMap=new HashMap<String,Term>(); // tag -> term in parse tree
	HashMap<String,Rule> ruleMap=new HashMap<String,Rule>(); // name -> Rule

	String start; // first rule name
	
	Action action=null;
	
	Term fault=null;
	
	Rule rule(String name) { return ruleMap.get(name); }

	// -----  compile a parse tree from the gist grammar into parse operators... ----

	void compile(Term tree) {
	// Gist = (xs (Rule/Import))* xs
		// Gist = (xs (Rule/Import) s ';'?)* xs
		if (!tree.isTag("Gist")) { fault(tree.toString()); return; }
		Term root=tree.child("Rule"); // start rule
		start=root.text("name");
		for (Term term: tree) {
			if (term.tag()=="Rule") {
				String name=term.text("name");
				if (termMap.get(name)!=null)
					fault("Duplicate rule definition: "+name);
				termMap.put(name,term);
			}
			if (term.tag()=="Import") importRule(term);
		}
		for (String name: termMap.keySet())
			compileRule(name,null,null);
	}
	
	void importRule(Term rule) {
		// Import = '_' s defn s label '._'? (s ','? s label '._'?)*
		for (Term label : rule)
			if (label.isTag("label")) {
				String grammar=label.text();
				Parser parser=getParser(grammar);
				if (parser==null)
					fault("Unknown grammar: "+grammar+"   Use: Gist.load(\""+grammar+"\",rules...) ");
				else imports.add(parser);
			}
	}
	
	Parser getParser(String grammar) {
		Parser parser=library.get(grammar);
		if (parser==null) {
			String rules=Library.get(grammar);
			if (rules!=null) parser=new Parser(rules);
			library.put(grammar,parser);
		}
		return parser;
	}
	
	Rule compileRule(String name,Term ref,Rule host) {
		// Rule = name dots? xs defn xs Sel 
		Rule rule=ruleMap.get(name);
		if (rule==null) {
			rule=new Rule(name); // undefined rule
			ruleMap.put(name,rule);
		}
		if (rule.body!=null) return rule; // already compiled
		Term term=termMap.get(name);
		if (term==null) {
			Rule target=null;
			for (Parser parser:imports) {
				target=parser.rule(name);
				if (target!=null) break;
			}
			if (target==null)
				fault(host,ref,"undefined reference");
			else {
				rule=target;
				ruleMap.put(name,rule);
			}
		} else if (term.tag=="Rule") {
			rule.elide=term.child("dots")!=null;
			rule.term=term.has("defn",":");
			rule.body=compileSel(term.child("Sel"),rule);
		}
		return rule;
	}

	ParseOp compileRef(Term ref, Rule host) {
		// Ref = name -- Boot
		// Ref = name ('.' name)* dots? / anon / skip / break
        if (ref.has("skip")) return new WhiteSpace(); // ~ => xs..*
        if (ref.has("break")) return new NewLine(); // $ => XML1.1 eol
        if (ref.has("anon")) return new Chars(new int[] {0,0x10FFFF});  // _ => any
		if (ref.child("name").next("name")!=null) return externalRef(ref,host);
		// normal local name....
		boolean elide=ref.has("dots");
		String name=ref.text("name");
		Rule rule=ruleMap.get(name); // compileRule finds reference..;
		if (rule==null) rule=compileRule(name,ref,host);
		if (rule.body!=null) { // rule has been compiled...
			if (!rule.fixed) host.fixed=false;
			if (elide || host.term || rule.elide) return rule.body;
			return new Ref(name,elide,host,ruleMap,rule);
		} // else rule may be undefined or in a ref loop
        	host.fixed=false; // safe, unknown resolution may be LR...
		return new Ref(name,elide,host,ruleMap,null);
	}

	ParseOp externalRef(Term ref, Rule host) {
		// Ref = name ('.' name)* dots? 
		boolean elide=ref.has("dots");
		String grammar=ref.text("name");
		Term t=ref.child().next();
		while (t.next()!=null) {grammar+="."+t.text(); t=t.next(); }
		String name=t.text();
		Parser parser=getParser(grammar);
		if (parser==null) {
			fault("Unknown grammar: "+grammar+"   Use: Gist.load(\""+grammar+"\",rules...) ");
			return new Ref(name,elide,host,ruleMap,null); // empty, continue compile
		}
		Rule rule=parser.rule(name);
		if (rule==null) {
			fault(host,ref,"undefined external rule...");
			return new Ref(name,elide,host,ruleMap,null); // empty, continue compile
		}
		if (!rule.fixed) host.fixed=false;
		if (elide || host.term || rule.elide) return rule.body;
		return new Ref(name,elide,host,ruleMap,rule);
	}

	ParseOp compileSel(Term sel,Rule host) {
		// Sel = Seq (xs '/' xs Seq)* 
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		for (Term term: sel) if (term.tag()=="Seq") {
			ParseOp arg = compileSeq(term,host);
			if (!union(args,arg)) args.add(arg);
		}
		if (args.size()==1) return args.get(0);
		for (int i=0; i<args.size()-1; i++) {
			if (Verify.vacant(args.get(i))) // x* / y  => fault
				return fault(host,sel,"can't reach all options...");
		}
		return new Sel(args.toArray(new ParseOp[0]));
	}

	boolean union(ArrayList<ParseOp> args, ParseOp w) {
		int size=args.size();
		if (size==0) return false;
		ParseOp v=args.get(size-1);
		if (v instanceof Chars && w instanceof Chars) {
			args.set(size-1,((Chars)v).union((Chars)w));
			return true;
		}
		return false;
	}

	ParseOp compileSeq(Term seq, Rule host) {
		// Seq = (sp Fact Rep?)+  -- Boot
		// Seq = Item (s (',' xs)? Item)*  
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		for (Term term: seq) {
			if (term.tag()=="Item") {
				ParseOp item=compileItem(term,host);
				if (item instanceof Seq) // flatten...
					for (ParseOp op:(((Seq)item).args)) args.add(op);
				else args.add(item);
			} else if (term.isTag("Fact")) // Boot..
				args.add(compileFact(term,host));
			else if (term.isTag("Rep")) {
				int i=args.size()-1;
				args.set(i,compileRepeat(args.get(i),term));
			}
		}
		for (int i=0;i<args.size()-1;i++) { // reduce !x y ....
			ParseOp x=args.get(i);
			if (isNegateChar(x)) { // !x 
				Chars cx=(Chars)((Negate)x).arg;
				ParseOp y=args.get(i+1);
				if (y instanceof Chars) { // !x y => y.exclude(x)
					args.set(i,((Chars)y).exclude(cx));
					args.remove(i+1);
				} else if (isNegateChar(y)) {  // !x !y => !(x/y)
					Chars cy=(Chars)((Negate)y).arg;
					args.set(i,new Negate(cx.union(cy)));
					args.remove(i+1);
					i-=1; // try for more...
				}
			}
		}
		if (args.size()==1) return args.get(0);
		for (int i=0; i<args.size()-1; i++) {
			if (Verify.overruns(args.get(i),args.get(i+1))) { // x* y  => fault if y>x
				//System.out.println("overrun? "+args);
				return fault(host,seq,"overrun: will fail after greedy one-match repeat...");
			}
		}
		return new Seq(args.toArray(new ParseOp[0]));
	}

	ParseOp compileItem(Term item, Rule host) {
		// Item = Factor Rep? / Prime
		if (item.has("Prime")) return compilePrime(item.child("Prime"),host);
		ParseOp factor=compileFactor(item.child("Factor"),host);
		if (item.has("Rep")) return compileRepeat(factor,item.child("Rep"));
		return factor;
	}

	ParseOp compileFactor(Term factor, Rule host) {
		// Factor = Fact (minus Fact)*
		Term fx=factor.child();
		if (!factor.has("minus")) return compileFact(fx,host);
		Term minus=fx.next();
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		ParseOp x=compileFact(fx,host); // x-y1-y2...
		while (minus!=null) {
			ParseOp y=compileFact(minus.next(),host);
			if (x instanceof Chars && y instanceof Chars)
				x=((Chars)x).exclude((Chars)y);
			else // x = (x-y) / z = (!y x)
				args.add(new Negate(y));
			minus=minus.next().next();
		}
		args.add(x);
		if (args.size()==1) return args.get(0);
		return new Seq(args.toArray(new ParseOp[0]));
	}

	boolean isNegateChar(ParseOp x) { // !'x'
		return (x instanceof Negate && ((Negate)x).arg instanceof Chars);
	}

	ParseOp compileRepeat(ParseOp factor, Term rep) {
		// Rep  = '+' / '?' / '*' Ints?
		if (rep.isText("*")) return new Repeat(factor,0,-1);
		if (rep.isText("+")) return new Repeat(factor,1,-1);
		if (rep.isText("?")) return new Repeat(factor,0,1);
		// Ints => int ('..' (int/anon))?
		Term ints=rep.child("Ints");
		int min=Integer.parseInt(ints.text("int"));
		Term hi=ints.child().next(); // int..(int/anon)
		if (hi==null) return new Repeat(factor,min,min); // x*min
		if (hi.tag()=="anon") return new Repeat(factor,min,-1);
		int max=Integer.parseInt(hi.text());
		return new Repeat(factor,min,max); // x*min..max
	}

	ParseOp compileFact(Term fact, Rule host) {
		// Fact = Ref/Literal/Group/Prior/Event
		Term x=fact.child();
		String tag=x.tag();
		if (tag=="Ref") return compileRef(x,host);
		if (tag=="Literal") return compileLiteral(x,host);
		if (tag=="Group") return compileGroup(x,host);
		if (tag=="Prior") return compilePrior(x,host);
		if (tag=="Event") return new Event(x,host);
		throw new UnsupportedOperationException("Fact "+x);
	}

	ParseOp compilePrime(Term prime, Rule host) {
		// Prime = Option/Many/Not/Peek
		Term x=prime.child();
		String tag=x.tag();
		if (tag=="Option") return compileOption(x,host);
		if (tag=="Many") return compileMany(x,host);
		if (tag=="Not") return compileNot(x,host);
		if (tag=="Peek") return compilePeek(x,host);
		throw new UnsupportedOperationException("Prime "+x);
	}

	ParseOp compileExp(Term exp, Rule host) {
		// Exp = (Sel xs)*
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		for (Term sel: exp)
			if (sel.tag()=="Sel") 
				args.add(compileSel(sel,host));
		if (args.size()==1) return args.get(0);
		return new Seq(args.toArray(new ParseOp[0]));
	}

	ParseOp compileGroup(Term term, Rule host) {
		// Group  = '(' Exp ')'  or  Boot: '('  Sel ws')'
		// Exp     = (Sel xs)*
		if (term.has("Sel")) return compileSel(term.child("Sel"),host);
		return compileExp(term.child("Exp"),host);
	}

	ParseOp compileOption(Term term, Rule host) {
		// Option  = '[' Exp ']'
		ParseOp factor=compileExp(term.child("Exp"),host);
		return new Repeat(factor,0,1);
	}

	ParseOp compileMany(Term term, Rule host) {
		// Many  = '{' Exp '}'
		ParseOp factor=compileExp(term.child("Exp"),host);
		return new Repeat(factor,0,-1);
	}

	ParseOp compileNot(Term neg, Rule host) {
		// Not = '!' blank* Factor
		ParseOp factor=compileFactor(neg.child("Factor"),host);
		return new Negate(factor);
	}

	ParseOp compilePeek(Term peek, Rule host) {
		// Peek = '&' blank* Factor
		ParseOp factor=compileFactor(peek.child("Factor"),host);
		return new Peek(factor);
	}

	ParseOp compilePrior(Term prior, Rule host) {
		// Prior = '@' blank* name
		Term name=prior.child("name");
		host.fixed=false;
		return new Prior(name.text());
	}

	ParseOp compileLiteral(Term literal, Rule host) {
		// Literal = Quote / Code
		Term x=literal.child();
		String tag=x.tag();
		if (tag=="Quote") return compileQuote(x,host);
		if (tag=="Code") return compileCode(x,host);
		throw new UnsupportedOperationException("Literal "+tag);
	}

	ParseOp compileQuote(Term quote, Rule host) {
		// Quote   = quote ('..' quote)?
		String qs=quote.text();
		Term q=quote.child();
		String q1=q.text();
		if (q.next()==null)
			if (q1.length()==2) // ''
				return new Empty();
			else if (q1.length()==3) { // 'x'
				int n=q1.codePointAt(1);
				return new Chars(new int[] {n,n});
			} else { // 'xyz'
				ArrayList<ParseOp> seq=new ArrayList<ParseOp>();
				for (int i=1;i<q1.length()-1;i++) {
					int n=q1.codePointAt(i);
					seq.add(new Chars(new int[] {n,n}));
				}
				return new Seq(seq.toArray(new ParseOp[0]));
			}
		String q2=q.next().text(); // 'x'..'y'
		if (q1.length()==3 && q2.length()==3) {
			int n=q1.codePointAt(1);
			int m=q2.codePointAt(1);
			if (m<n) return fault(host,quote,"bad range");
			return new Chars(new int[] {n,m});
		}
		throw new UnsupportedOperationException("Quote "+qs);
	}

	ParseOp compileCode(Term code, Rule host) {
		// Code	= int ('..' int)?  Boot
		// Code = '0' ('x'/'X') Hexs
		Term range=code.child(); //  Hexs/Ints
		if (range.tag()=="int") range=code; // Boot 
		// Ints = int ('..' (int/anon))?
		// Hexs = hex ('..' hex)?
		int k=10; // Ints
		if (range.tag()=="Hexs") k=16;
		Term lo=range.child(); // lo..hi
		int i=Integer.valueOf(lo.text(),k);
		if (lo.next()==null) return new Chars(new int[] {i,i});
		int j; // i..j
		if (lo.next().tag()=="anon") j=0x10ffff;
		else j=Integer.valueOf(lo.next().text(),k);
		if (j<i) return fault(host,code,"bad range");
		return new Chars(new int[] {i,j});
	}

	ParseOp fault(Rule rule, Term term, String msg) {
		// Rule: <name> <term.text>: msg
		return fault("Rule: "+rule.name+"  "+term.text()+"\n    "+msg);
	}

	ParseOp fault(String msg) {
		Term log=new Term("-- "+msg,null,0,-1);
		log.next=fault;
		fault=log;
		return new Empty();
	}


	public String toString() {
		String s="grammar\n";
		for (Rule rule: ruleMap.values()) s+=rule.toString()+"\n";
		return s;
	}
		
} // Parser



