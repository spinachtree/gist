package org.spinachtree.gist;

import java.util.*;

abstract class ParseOp {
	abstract boolean parse(Scan scan);
}

abstract class Rule extends ParseOp {
	abstract String name();
}

class GistFault extends RuntimeException {
	GistFault(String s) { super(s); }
}

class Memo {

	int start; //=-1;
	Term result; //=null;
	boolean loop; //=false; // left recursive rule
	Term seed; // left recursion result
	ArrayList<Term> frames;

	Memo() {
		start=-1;
		result=null;
		loop=false;
		seed=null; // for leftParse
		frames=null; // stack only allocated if needed
	}
}

public class Scan {

	Scan(String src, int sot) {
		this(src,sot,src.length());
	}
	
	Scan(String src, int sot, int eot) {
		input=src;
		this.sot=sot;
		this.eot=eot; // default source.length();
		pos=sot; // default 0, parse char input cursor
		seed=new Term("<root>",src,0,eot);
		tip=seed;	// tree growth tip
	}
	
	String input; // source text (a String for now)
	int pos;      // cursor index in str
	int ch;       // current char code
	
	int sot;     // input start index
	int eot;     // input end index
	int peak;    // fail high water mark
	
	Term seed;  // Terms build out from seed.next
	Term tip;   // last output term
	
	int skip=0;
	
	Vent vent=null; // event interface
	
	Map<Rule,Memo> memos=new HashMap<Rule,Memo>();
	
	Term root() { return seed.next; }
	
	// public methods--------------------------------
	
	public int pos() { return pos; }
	
	public Term tip() { return tip; }

	public int codePoint() {
		if (pos>=eot) return -1;
		ch=input.codePointAt(pos);
		if (ch<33 && skip>0) {
			advance();
			return codePoint();
		}
		return ch;
	}

	public void advance() {
		if (ch<0x10000) pos+=1; else pos+=2;
	}
	
	
	// --- end of public methods -------------------
	
	Memo memo(Rule rule) {
	        Memo memo=memos.get(rule);
	        if (memo!=null) return memo;
	        memo=new Memo();
	        memos.put(rule,memo);
	        return memo;
	}
	
	void reset(int p, Term t) {
		if (pos>peak) peak=pos;
		tip=t;
		t.next=null;
		pos=p;
	}
	
	Term newTerm(String tag,int p,Term t) {
		// Term matching input text...
		Term term = new Term(tag,input,p,pos);
		term.child=t.next;
		term.prior=t;
		t.next=term;
		tip=term;
		return term;
	}
	
	static final Term FAIL=new Term("fail","",0,0);
	
	boolean apply(Term term) {
		if (term==FAIL) return false;
		// replica may be needed...
		Term replica = new Term(term.tag,input,term.sot,term.eot);
		replica.child=term.child;
		replica.prior=tip;
		tip.next=replica;
		tip=replica;
		pos=term.eot;
		return true;
	}
	
	// -- trace and fault reporting ------------------------------------
	
	String traceReport(String msg) {
		if (peak<pos) peak=pos;
		Term term=new Term(report(msg),input,pos,peak);
		return term.toString();
	}
	
	String trail() {
		Term t=tip; // last rule term
		List<String> tags=new ArrayList<String>();
		while (t!=null) { tags.add(t.tag()); t=t.prior; }
		if (tags.isEmpty()) return "";
		String report = "\n";
		int i=tags.size()-2; // ignore <root>
		if (i>8) { i=6; report+=" ... "; }
		while (i>=0) report+=tags.get(i--)+" ";
		return report;
	}

	String report(String msg) {
		return "-- "+msg+" ("+peak+" of "+eot+")"+trail();
	}

	Term faultResult(String msg) {
		if (peak<pos) peak=pos;
		seed.tag=report(msg); // -- msg...
		seed.sot=pos;
		seed.eot=peak;
		seed.child=seed.next;
		seed.next=null;
		return seed;
	}
	
} // Scan




