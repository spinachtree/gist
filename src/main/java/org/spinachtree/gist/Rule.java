package org.spinachtree.gist;

import java.util.*;

class Rule extends ParseOp {

	Rule(String name) {
		this.name=name;
		term=false;  // if rule : terminal string value
		body=null;   // ParseOp 
		elide=false; // ellipsis name.. rule
		fixed=true; // ok to use a memo
	}
		
	Rule(String name, ParseOp body, boolean term) {
		this(name);
		this.body=body;
		this.term=term;
	}

	Rule(String name, ParseOp body, boolean term, boolean elide, boolean fixed) {
		this(name,body,term);
		this.elide=elide;
		this.fixed=fixed;
	}

	String name;
	boolean term, elide, spaced, fixed;
	ParseOp body;
	
	Rule copy() { return new Rule(name,body,term,elide,fixed); }
	
	String name() { return this.name; }
	
	boolean parse(Scan scan) {
                Memo memo=scan.memo(this);
		int i=scan.pos;
		// first check for cache memo results....
		if (i==memo.start) { // deja vu
			if (memo.result==null) { // detect left recursion...
				memo.loop=true;
				memo.result=Scan.FAIL;
				return false;
			} else if (fixed || memo.loop) // ok to use a memo 
				return scan.apply(memo.result);
		}
		// normal parse (no result memo)....
		memo.start=i;
		memo.result=null;
		Term t=scan.tip;
		if (spaced) scan.skip++;
		if (body.parse(scan)) {
			if (spaced) scan.skip--;
			memo.start=i;
			memo.result=scan.newTerm(name,i,t);
			if (memo.loop) return leftParse(scan,memo,i,t);
			return true;
		}
		if (spaced) scan.skip--;
		memo.start=i;
		memo.result=Scan.FAIL;
		return false;
	}
	
	boolean leftParse(Scan scan, Memo memo, int i, Term t) { // left recusion
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
	
	public String toString() {
		String label = name;
		if (elide) label+="..";
		if (!fixed) label+="~ ";
		String defn = " = ";
		if (term) defn = " : ";
		if (body==null) return label+defn+"<null>";
		return label+defn+body.toString();
	}
	
} // Rule

