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

	boolean parse(Parser par) {
		return startRule().parse(par);
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
	
	boolean match(Parser par) {
//	        Memo memo=scan.memo(this);
		int p=par.pos;
//if (par.trace!=null)// && par.trace.equals(name))
//	System.out.println(name+" pos="+p);

//if (name.equals("rule")) System.out.println("rule: "+p);
		// first check for cache memo results....
//		if (i==memo.start) { // deja vu
//			if (memo.result==null) { // detect left recursion...
//				memo.loop=true;
//				memo.result=Scan.FAIL;
//				return false;
//			} else if (fixed || memo.loop) // ok to use a memo 
//				return scan.apply(memo.result);
//		}
		// normal parse (no result memo)....
//		memo.start=i;
//		memo.result=null;
		Term t=par.tip;
		if (body.parse(par)) {
//if (name.equals("rule")) System.out.println("rule: "+p+".."+par.pos+" "+
//par.input.substring(p,par.pos)+"\n"+t.next());
//			memo.start=i;
//			memo.result=scan.newTerm(name,i,t);
//			if (memo.loop) return leftParse(scan,memo,i,t);
			
			par.newTerm(name,p,t);
//if (par.trace!=null)// && par.trace.equals(name))
//	System.out.println(name+" match "+par.show(p,par.pos));
			return true;
		}
//		memo.start=i;
//		memo.result=Scan.FAIL;
//if (par.trace!=null)// && par.trace.equals(name))
//	System.out.println(name+" fail  "+par.show(p,par.peak));
		return false;
	}
/*	boolean match() {
		int i=par.pos;
		boolean result=body.parse();
		if (result) return true;
		par.reset(i);
		return result;
	}
*/	
	String me() { return name+(term?":":"=")+body+"\n"; }

}

// A pseudo Op to hold the list of rule names...  key "=" in ruleMap

/*class Rules extends Rule {
	Rules(List<String> ruleNames) {
		this.ruleNames=ruleNames;
		name=ruleNames.get(0);
	}
	List<String> ruleNames;
	public String toString() {
		String str="";
		for (String name:ruleNames) str+=name+" ";
		return str;
	}
}
*/

