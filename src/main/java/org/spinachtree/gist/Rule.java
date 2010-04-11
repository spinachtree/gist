package org.spinachtree.gist;

import java.util.*;

class Rules extends Op {
	Rules() {}

	List<String> ruleNames=new ArrayList<String>();
	Map<String,Rule> ruleMap=new HashMap<String,Rule>();

	boolean isEmpty() { return ruleNames.isEmpty(); }
	void add(String name) { ruleNames.add(name); }
	
	Rule getRule(String name) { return ruleMap.get(name); }
	Rule putRule(String name, Rule rule) { return ruleMap.put(name,rule); }
	
	String start() { return ruleNames.get(0); }
	Rule startRule() { return getRule(start()); }

	Action action=null; // event interface

	Term parse(String text) {
		Scan scan=new Scan(text);
		Rule rule=startRule();
		boolean result=rule.parse(scan);
		if (!result) return scan.faultResult(rule.name+" parse failed... "); 
		if (scan.pos<scan.eot) return scan.faultResult(rule.name+" parse incomplete... "); 
		return scan.root(); //seed.next;
	}
	
	public String toString() {
		String str="";
		for (String name:ruleNames) str+=ruleMap.get(name).toString();
		return str;
	}
}


class Rule extends Op {
	Rule() {}
	Rule(String name, boolean elide, boolean term, Op body) {
		this.name=name;
		this.elide=elide; //"..".equals(elide);
		this.body=body;
		this.term=term; //":".equals(defn);
	}
	
	String name;
	boolean elide=false, term=false, fixed=true;
	Op body;
	
	Op copyMe() { crash(); return this; }
	
	boolean match(Scan scan) {
	        Memo memo=scan.memo(this);
		int p=scan.pos;
		// first check for cache memo results....
		if (p==memo.start) { // deja vu
			if (memo.result==null) { // detect left recursion...
				memo.loop=true;
				memo.result=scan.FAIL;
				return false;
			} else if (fixed || memo.loop) { // ok to use a memo
				return scan.apply(memo.result); 
			}
		}
		// normal parse (no result memo)....
		memo.start=p;
		memo.result=null;
		Term t=scan.tip;
		if (body.parse(scan)) {
			memo.start=p;
			memo.result=scan.newTerm(name,p,t);
			if (memo.loop) return leftParse(scan,memo,p,t);
			return true;
		}
		memo.start=p;
		memo.result=scan.FAIL;
		return false;
	}
	
	boolean leftParse(Scan scan,Memo memo,int i,Term t) { // left recusion
		if (memo.seed!=null) { // re-entrant for (errant) nesting...
		    if (memo.frames==null) memo.frames=new ArrayList<Term>();
		    memo.frames.add(memo.seed); // stack nested results
		}
		memo.seed=memo.result; // seed result to grow...
		int j=i, best=scan.pos;
		while (true) { // repeat parse to grow the seed result....
			scan.reset(i,t);
			memo.result=memo.seed;
			memo.start=memo.result.sot;
			if (!body.parse(scan)) break;
			j=scan.pos;
			if (j<=best) break;
			best=j;
			memo.seed=scan.newTerm(name,i,t);
		}
		memo.result=memo.seed;
		memo.seed=null;
		if (memo.frames!=null && !memo.frames.isEmpty())
		    memo.seed=memo.frames.remove(memo.frames.size()-1);
		memo.start=memo.result.sot;
		scan.reset(i,t);
		return scan.apply(memo.result);
	}

	String me() { return name+(fixed?"":"+")+(term?":":"=")+body+"\n"; }

}


