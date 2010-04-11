package org.spinachtree.gist;

import java.util.*;

/**
Input scanner, not usually needed for Gist application users.

<p>The Scan contains the input text, cursor, parse tree term, etc.
Normally for private use internal to the parser, but an action
event handler is given access via public methods.
</p>

*/

public class Scan {
	
	Scan(String src) {
		input=src;
		eot=src.length();
		pos=0;
		chr=src.codePointAt(pos);
		seed=new Term("<root>",src,pos,eot);
		tip=seed; // tree growth tip
		peak=0;
		top=null;
		memos=new HashMap<Rule,Memo>();
	}
	
	// parse context....................
	
	String input; // source text (a String for now)
	int pos;      // cursor index in str
	int chr;      // current char code
	
	int sot;     // input start index
	int eot;     // input end index
	int peak;    // fail high water mark
	
	Term seed;  // Terms build out from seed.next
	Term tip;   // last output term
	Term top;   // peak output term
	
	Map<Rule,Memo> memos;
	
	// public interface methods for event actions...............
	
	/**
	position of cursor index in the input scan
	<p>This index may not be a character index. The character encoding
		may use a byte index, and even in a Java string the use
		of surrogate pairs the index is not one-to-one with chars.

	@return cursor position
	*/
	public int pos() { return pos; }
	
	/**
	last term created
	<p>Terms prior to the event, but the parent host rule will
		not yet exist (nor any other incomplete parents).

	@return last term
	*/
	public Term tip() { return tip; }

	/**
	current char code
	<p>char at current cursor position

	@return int char point code
	*/
	public int chr() { return chr; }

	/**
	advance to the next character position
	<p>cursor position will increase by one or more

	*/
	public void advance() {
		if (chr<0x10000) pos+=1; else pos+=2;
		if (pos>=eot) chr=-1;
		else chr=input.codePointAt(pos);
	}

	/**
	create a new parse tree node
	<p>matches text from the given position to current cursor,
	nodes after the given term become children of the new node
	and the new node becomes the next node after the given term.

	@param p   input cursor start position
	@param t   tip of parse tree prior to new node

	@return new term
	*/
	public Term newTerm(String tag,int p,Term t) {
		// Term matching input text...
		Term term = new Term(tag,input,p,pos);
		term.child=t.next;
		term.prior=t;
		t.next=term;
		tip=term;
		return term;
	}
	
	/**
	reset input cursor and parse tree
	<p>to backtrack after a failure

	@param p   input curson pos
	@param t   tip of parse tree

	*/
	public void reset(int p, Term t) {
		if (p>=eot) return;
		if (pos>peak) { peak=pos; top=tip;}
		tip=t;
		t.next=null;
		if (pos==p) return;
		pos=p;
		chr=input.codePointAt(pos);
	}
	
	/**
	get root node
	<p>retrieve root node after the parse is complete

	@return root term
	*/
	Term root() { return seed.next; }
	

	// Memos ---------------------------------------------------------

	Memo memo(Rule rule) {
	        Memo memo=memos.get(rule);
	        if (memo!=null) return memo;
	        memo=new Memo();
	        memos.put(rule,memo);
	        return memo;
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
		if (pos>=eot) chr=-1;
		else chr=input.codePointAt(pos);
		return true;
	}
	

	// -- trace and fault reporting ------------------------------------

	String traceReport(String msg) {
		if (peak<pos) peak=pos;
		Term term=new Term(report(msg),input,pos,peak);
		return term.toString();
	}

	String report(String msg) {
		String report = "-- "+msg+" ("+peak+" of "+eot+")\n";
		Term t=tip; // last rule term
		if (top!=null) t=top;
		List<String> tags=new ArrayList<String>();
		while (t!=null) { tags.add(t.tag()); t=t.prior; }
		if (tags.isEmpty()) return report;
		int i=tags.size()-2; // ignore <root>
		if (i>8) { i=6; report+=" ... "; }
		while (i>=0) report+=tags.get(i--)+" ";
		return report;
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

class GistFault extends RuntimeException {
	GistFault(String s) { super(s); }
}

