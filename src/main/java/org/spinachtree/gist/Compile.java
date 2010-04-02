package org.spinachtree.gist;

import java.util.*;

class Compile {
	
	// compile parse tree => parser Ops ................................
	
	Compile(Transform transform,Rules rules) {
		this.transform=transform;
		this.rules=rules;
	}
	
	Transform transform;
	Rules rules; // resultant compiled rules

	Object xla; // instance of transform methods
	Term faults=null;
	Map<String,Term> termMap=new HashMap<String,Term>();

	void rules(Object xla,Term term) {
		this.xla=xla;
		if (!term.isTag("rules")) throw new GistFault("Grammar rules parse fault:\n"+term);
		mapTerms(term);
		if (rules.isEmpty()) throw new GistFault("\nNo 'rule' terms found...");
		buildRules();
		if (faults!=null) throw new GistFault(faults.toString());
	}
	
	void mapTerms(Term term) {
		for (Term t:term) {
			if (t.isTag("rule")) {
				String name=t.text("name");
				rules.add(name);
				Term pre=termMap.get(name);
				if (pre==null) termMap.put(name,t);
				else faultTerm("Duplicate rule: "+name);
			} else if (t.isTag("import")) importRule(t);
		}
	}

	void buildRules() {
		for (String name:termMap.keySet()) {
			Rule rule=rules.getRule(name);
			if (rule==null) buildRule(name);
		}
	}

	Stack<String> buildStack=new Stack<String>();

	Rule buildRule(String name) {
		Rule rule=rules.getRule(name);
		if (rule!=null) return rule; // already built (or being built)
		Term term=termMap.get(name);
		if (term==null) {
			rule=importRef(name);
			if (rule!=null) {
				rules.putRule(name,rule);
				return rule;
			}
			faultTerm("Missing rule: "+name);
			rule=new Rule(name,false,false,null);
			rules.putRule(name,rule);
			return rule;
		}
		rule=new Rule(name,term.has("elide"),term.has("defn",":"),null);
		rules.putRule(name,rule); // target for refs during build...
		buildStack.push(name);
		try { rule.body=(Op)transform.build(xla,term.child("sel")); }
		catch (Exception e) { throw new GistFault("buildRule "+buildStack+"\n"+e); }
		buildStack.pop();
		return rule;
	}

	String hostName() { return buildStack.peek(); }
	Rule hostRule() { return rules.getRule(hostName()); }
	
	Op ruleRef(String name) { return ruleRef(null,name,null); }

	Op ruleRef(String path, String name, String elide) {
		if (path!=null && path.length()>0) return externalRef(path,name,elide);
		Rule host=hostRule();
		Rule rule=buildRule(name);
		Op op=inline(host,rule);
		if (op!=null) return op;
		return new Ref(rules,host,rule,(elide!=null));
	}
	
	Op inline(Rule host,Rule rule) { // macro expansion copy...
		if (host.term && rule.body!=null) {
			Op bod=rule.body.copy();
			if (bod!=null) return bod;
		}
		return null;
	}

	Term faultTerm(String msg) { 
		Term term=new Term("-- "+msg,null,0,-1);
		term.next=faults;
		faults=term;
		return term;
	}

	// External rules----------------------------------------------------------------------
	
	List<Gist> imports=new ArrayList<Gist>();

	void importRule(Term impt) {
		// import  = '_' s defn s label '._'? (s ','? s label '._'?)*
		for (Term label_ : impt)
			if (label_.isTag("label")) {
				String label=label_.text();
				Gist gist=Library.get(label);
				if (gist==null) noGrammar(label);
				else imports.add(gist);
			}
	}

	Rule importRef(String name) {
		for (Gist gist:imports) {
			Rule rule=gist.getRule(name);
			if (rule!=null) return rule;
		}
		return null;
	}

	Op externalRef(String path, String name, String elide) {
		// ref = path name elide? 
		String label=path.substring(0,path.length()-1); // trim final '.'
		Gist gist=Library.get(label);
		if (gist==null) { noGrammar(label); return new False(); }
		Rule rule=gist.getRule(name);
		if (rule==null) {
			faultTerm("Undefined external rule: "+path+name);
			return new False();
		}
		return new Ref(rules,hostRule(),rule,(elide!=null));
	}

	void noGrammar(String label) {
		faultTerm("Unknown grammar: "+label+" Use: Gist.load(\""+label+"\",rules...) ");
	}

} // Compile

