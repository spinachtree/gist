
package org.spinachtree.gist;

import java.util.*;


class Parser {
	
	// Full Gist language grammar, but expressed using Boot grammar language...
	// Boot does not accept: comments, not !, multi-line rules, name.name refs, ..

	public static final String gistGrammar = rules(
		"Gist    = (xs (Rule/Import))* xs ",
		"Rule    = name xs defn xs Body ",
		"Body    = Seq (xs '/' xs Seq)* ", 
		"Seq     = (Op Factor Repeat?)+ ", 
		"Op      = s ((','/minus) s)? ", 
		"Repeat  = '+' / '?' / '*' Ints? ", 
		"Factor  = Ref/Literal/Group/Option/Many/Not/Peek/Prior/Event ", 
		"Group   = '(' Exp ')' ", 
		"Option  = '[' Exp ']' ", 
		"Many    = '{' Exp '}' ",
		"Exp     = (Body xs)* ",
		"Not     = '!' s Factor / Ref s '-' s Ref ", 
		"Peek    = '&' s Factor ", 
		"Prior   = '@' s name ", 
		"Event   = '<' name? s args? '>' ", 
		"Literal = Quote / Code ", 
		"Quote   = quote ('..' quote)? ", 
		"Code    = '0' ('x'/'X') Hexs / Ints ", 
		"Ints    = int ('..' (int/anon))? ", 
		"Hexs    = hex ('..' ('0'('x'/'X'))? hex)? ", 
		"Ref     = name dots? / tilde / dollar / anon ", 
		"Import  = (name/anon) xs '->' xs uri ",
		"uri     = label ('#' name)? ",
		"name    : alpha (alnum/'_')* ",
		"defn    : '=' / ':' ", 
		"int     : digit+ ", 
		"hex     : (digit/'a'..'f'/'A'..'F')+ ", 
		"quote   : 39 (32..38/40..126)* 39 ", 
		"s       : blank* ", 
		"xs      : (sp* ';'? comment?)* ", 
		"comment : ('--' / '//' / '#') print* ",
		"label   : (33/36..126)+ ", // uri (graph-'#'-'"')
		"args    : (33..61/63..126)+ ", // Event (graph-'>')
		"alnum   : alpha/digit ", 
		"alpha   : 'a'..'z'/'A'..'Z' ", 
		"digit   : '0'..'9' ", 
		"dot     : '.' ", 
		"dots    : '..' ", 
		"colon   : ':' ", 
		"minus   : '-' ", 
		"tilde   : '~' ", 
		"dollar  : '$' ", 
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
		if (vent!=null) scan.vent=vent;
		boolean result=ruleMap.get(start).parse(scan);
		if (!result) return scan.faultResult(start+" parse failed... "); 
		if (scan.pos<scan.eot) return scan.faultResult(start+" parse incomplete... "); 
		return scan.root();
	}

	List<String> imports=new ArrayList<String>();

	HashMap<String,Term> termMap=new HashMap<String,Term>(); // tag -> term in parse tree
	HashMap<String,Rule> ruleMap=new HashMap<String,Rule>(); // name -> Rule

	String start; // first rule name
	
	Vent vent=null;
	
	Term fault=null;
	
	Rule rule(String name) { return ruleMap.get(name); }

	// -----  compile a parse tree from the gist grammar into parse operators... ----

	void compile(Term tree) {
		// Gist = xs (xs Rule/Import)* xs
		if (!tree.isTag("Gist")) { fault(tree.toString()); return; }
		Term root=tree.child("Rule"); // start rule
		start=root.text("name");
		for (Term term: tree) {
			String tag=term.tag();
			if (tag=="Rule" || tag=="Import") {
				if (term.child("name")!=null) {
					String name=term.text("name");
					if (termMap.get(name)!=null)
						fault("Duplicate rule definition: "+name);
					termMap.put(name,term);
				}
			}
			if (tag=="Import") { // name/anon -> uri
				Term uri=term.child("uri");
				if (term.has("anon") && uri.has("name")) // _ -> label#name
					fault("Drop #"+uri.text("name")+" from: "+term.text());
				String label=uri.text("label");
				imports.add(label);
			}
		}
		for (String label:imports) {
			Parser parser=library.get(label);
			if (parser==null) {
				String grammar=Library.get(label);
				if (grammar!=null) parser=new Parser(grammar);
				library.put(label,parser);
			}
			if (parser==null) fault("Can't find grammar: "+label);
		}
		for (String name: termMap.keySet())
			compileRule(name,null,null);
	}

	Rule compileRule(String name,Term ref,Rule host) {
		// Rule = inset name sp defn sp Body
		Rule rule=ruleMap.get(name);
		if (rule==null) {
			rule=new Rule(name); // undefined rule
			ruleMap.put(name,rule);
		}
		if (rule.body!=null) return rule; // already compiled
		Term term=termMap.get(name);
		if (term==null) {
			for (String label:imports) {
				Parser parser=library.get(label);
				if (parser!=null) {
					rule=parser.rule(name);
					if (rule!=null) { // import into this grammar
						ruleMap.put(name,rule);
						return rule;
					}
				}
			}
			fault(host,ref,"undefined reference");
			rule=new Rule(name); // empty, continue compile
		} else if (term.tag=="Rule") {
			//rule.elide=term.child("dots")!=null;
			rule.term=term.has("defn",":");
			rule.body=compileChoice(term.child("Body"),rule);
		} else if (term.tag=="Import") { //  name -> uri
			// Import  = (name/anon) xs '->' xs uri
			// uri = label ('#' name)?
			Term uri=term.child("uri");
			Parser parser=library.get(uri.text("label"));
			if (parser!=null) {
				if (uri.child("name")==null || uri.text("name").equals(name)) {
					Rule target=parser.rule(name);
					if (target!=null) rule.body=target.body;
				} else rule.body=parser.rule(uri.text("name")); // #name
				if (rule.body==null) fault(rule,uri,"Rule could not be found...");
			}	
		}
		return rule;
	}

	ParseOp compileRef(Term ref, Rule host) {
		// Ref   = name -- Boot
		// Ref   = name dots? / tilde / dollar / anon
                if (ref.has("tilde"))
                        return new WhiteSpace(); // ~ => xs..*
                if (ref.has("dollar"))
                        return new NewLine(); // $ => XML1.1 eol
                if (ref.has("anon"))
                        return new Chars(new int[] {0,0x10FFFF});  // _ => any
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

	ParseOp compileChoice(Term body,Rule host) {
		// Body = Seq (sp '/' Seq)*
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		for (Term term: body) if (term.tag()=="Seq") {
			ParseOp arg = compileSeq(term,host);
			if (!union(args,arg)) args.add(arg);
		}
		if (args.size()==1) return args.get(0);
		for (int i=0; i<args.size()-1; i++) {
			if (Verify.vacant(args.get(i))) // x* / y  => fault
				return fault(host,body,"can't reach all options...");
		}
		return new Select(args.toArray(new ParseOp[0]));
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
		// Seq = (Op Factor Repeat?)+
		// Op  = sp ((','/minus) xs)?
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		Term term=seq.child();
		while (term!=null) {
			String tag=term.tag();
			if (tag=="Factor")
				term=addFactorRepeat(args,term,host);
			else if (tag=="Op") {
				if (term.has("minus")) { // w-x => !x w
					int i=args.size();
					if (i==0)
						return fault(host,seq,"unexpected unary -x: w-x => !x w ...");
					ParseOp w=args.remove(--i);
					term=addFactorRepeat(args,term.next(),host); // x 
					args.set(i,new Negate(args.get(i))); // !x
					args.add(w); // !x w
				} else term=term.next();
			} else if (tag=="sp") term=term.next(); // Boot
			else throw new UnsupportedOperationException("Seq "+term);
		}
		for (int i=0;i<args.size()-1;i++) { // reduce !x y ....
			ParseOp x=args.get(i);
			if (isNegateChar(x)) { // !x 
				Chars cx=(Chars)((Negate)x).arg;
				ParseOp y=args.get(i+1);
				if (y instanceof Chars) { // !x y => y-x
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
				return fault(host,seq,"overrun: will fail after greedy one-match repeat...");
			}
		}
		return new Seq(args.toArray(new ParseOp[0]));
	}

	Term addFactorRepeat(ArrayList<ParseOp> args,Term term,Rule host) {
		ParseOp factor=compileFactor(term,host);
		term=term.next();
		if (term!=null && term.tag()=="Repeat") {
			factor=compileRepeat(factor,term);
			term=term.next();
		}
		args.add(factor);
		return term;
	}

	boolean isNegateChar(ParseOp x) { // !'x'
		return (x instanceof Negate && ((Negate)x).arg instanceof Chars);
	}

	ParseOp compileRepeat(ParseOp factor, Term rep) {
		// Repeat  = '+' / '?' / '*' Ints?
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

	ParseOp compileFactor(Term factor, Rule host) {
		// Factor = Ref / Literal / Phrase / Not / Peek / Prior
		Term x=factor.child();
		String tag=x.tag();
		if (tag=="Ref") return compileRef(x,host);
		if (tag=="Literal") return compileLiteral(x,host);
		if (tag=="Phrase") return compilePhrase(x,host); // Boot
		if (tag=="Group") return compileGroup(x,host);
		if (tag=="Option") return compileOption(x,host);
		if (tag=="Many") return compileMany(x,host);
		if (tag=="Not") return compileNot(x,host);
		if (tag=="Peek") return compilePeek(x,host);
		if (tag=="Prior") return compilePrior(x,host);
		if (tag=="Event") return new Event(x,host);
		throw new UnsupportedOperationException("Factor "+x);
	}

	ParseOp compilePhrase(Term phrase, Rule host) {
		// Phrase  = '(' Body ws ')' -- Boot
		return compileChoice(phrase.child("Body"),host);
	}

	ParseOp compileExp(Term exp, Rule host) {
		// Exp = (Body xs)*
		ArrayList<ParseOp> args=new ArrayList<ParseOp>();
		for (Term body: exp)
			if (body.tag()=="Body") 
				args.add(compileChoice(body,host));
		if (args.size()==1) return args.get(0);
		return new Seq(args.toArray(new ParseOp[0]));
	}

	ParseOp compileGroup(Term term, Rule host) {
		// Group  = '(' Exp ')' 
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



