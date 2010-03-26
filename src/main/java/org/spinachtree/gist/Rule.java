package org.spinachtree.gist;

import java.util.*;

class Rule extends Op {
	Rule() {}
	Rule(Parser par, String name, boolean elide, boolean term, Op body) {
		this.par=par;
		this.elide=elide; //"..".equals(elide);
		this.name=name;
		this.body=body;
		this.term=term; //":".equals(defn);
	}
	
	String name;
	boolean elide=false, term=false, fixed=true;
	Op body;
	
	Op copyMe() { crash(); return this; }
	
	boolean match() {
//	        Memo memo=scan.memo(this);
		int p=par.pos;
if (par.trace!=null)// && par.trace.equals(name))
	System.out.println(name+" pos="+p);

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
		if (body.parse()) {
//if (name.equals("rule")) System.out.println("rule: "+p+".."+par.pos+" "+
//par.input.substring(p,par.pos)+"\n"+t.next());
//			memo.start=i;
//			memo.result=scan.newTerm(name,i,t);
//			if (memo.loop) return leftParse(scan,memo,i,t);
			
			par.newTerm(name,p,t);
if (par.trace!=null)// && par.trace.equals(name))
	System.out.println(name+" match "+par.show(p,par.pos));
			return true;
		}
//		memo.start=i;
//		memo.result=Scan.FAIL;
if (par.trace!=null)// && par.trace.equals(name))
	System.out.println(name+" fail  "+par.show(p,par.peak));
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
	String me() { return name+(term?":":"=")+body; }

}

// A pseudo Op to hold the list of rule names...  key "=" in ruleMap

class Rules extends Rule {
	Rules(Parser par, List<String> ruleNames) {
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


