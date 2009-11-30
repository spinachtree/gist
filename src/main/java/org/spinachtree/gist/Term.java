package org.spinachtree.gist;

import java.util.*;

/**
<p>Term represents a parse tree node.</p>
<p> 
An application can navigate through Terms in a parse tree, and access text matched by the grammar rules.</p>
<p>
The Gist parser returns the Term for the root of the parse tree, which will have a tag name corresponding to the start rule for the grammar, or a tag that reports a fault if the parse failed or did not consume all the input text. The tree of Terms is immutable and there is no public constructor.</p>

<pre>
  The basic methods:

  tag()   -- the name of the gist grammar rule that generated this term.
  text()  -- the string matched by this term.
  child() -- first child term, or null for a terminal leaf node.
  next()  -- next sibling term, or null.

</pre>

*/

public class Term implements Iterable<Term> {

	String tag; 	// name of rule that generated this Term
	String text;	// input text string
	int sot;		// sart-of-text text index
	int eot;		// end-of-text text index
	
	Term child;	// first in linked list of children
	Term next;	// following sibling link
	Term prior;	// prior sibling, or parent
	
	Object bag; // for app use, not used by parser
	
	Term(String tag, String text, int sot, int eot) {
		this.tag=tag.intern();
		this.text=text;
		this.sot=sot;
		this.eot=eot;
		child=null;
		next=null;
		prior=null;
		bag=null;
	}

	/**
	The tag name is a grammar rule name.
	<p>
	The tag name is the name of the gist grammar rule that generated this parse tree term, 
	or a fault report message.</p>
	<p>
	The tag name is an intern() String instance so that == can be used
	with "literal.." strings (but use equals() with other strings).</p>

	@return  tag name of this term
	*/
	public String	tag() { return tag; }
	
	/**
	String of text input matched by this term.
	@return the matched text string
	*/
	public String	text() {
		if (sot<0 || eot<sot) return "";
		return text.substring(sot,eot);
	}

	/**
	String matched by first child with the given tag name if any.
	@param tag   tag name for a child
	@return the text matched by the first child with that tag name, or ""
	*/
	
	public String text(String tag) {
		Term kid=child(tag);
		if (kid==null) return "";
		return kid.text();
	}
	
	/**
	First child term.
	@return the first child term, or null
	*/
	public Term child() { return child; }
	
	/**
	First child with given tag name.
	@param tag   tag name of child
	@return the first child term with this tag name, or null
	*/
	public Term child(String tag) {
		if (child==null) return null;
		String id=tag.intern();
		if (child.tag==id) return child;
		Term nxt=child.next;
		while (nxt!=null) {
			if (nxt.tag==id) return nxt;
			nxt=nxt.next;
		}
		return null;
	}
	
	/**
	next sibling term.
	@return the next sibling, or null
	*/
	public Term next() { return next; }
	
	/**
	next sibling with given tag name.
	@param tag   tag name for next sibling
	@return the next tag named term, or null
	*/
	public Term next(String tag) { 
		if (next==null) return null;
		String id=tag.intern();
		Term nxt=next;
		while (nxt!=null) {
			if (nxt.tag==id) return nxt;
			nxt=nxt.next;
		}
		return null;
	}	

	/**
	prior sibling term.
	@return the prior sibling, or null
	*/
	public Term prior() { return prior; }
	
	/**
	prior sibling with given tag name.
	@param tag   tag name for next sibling
	@return the next prior tag named term, or null
	*/
	public Term prior(String tag) { 
		if (prior==null) return null;
		String id=tag.intern();
		Term nxt=prior;
		while (nxt!=null) {
			if (nxt.tag==id) return nxt;
			nxt=nxt.prior;
		}
		return null;
	}	

	/**
	parent term.
	@return the parent term, or null for the root term
	*/
	public Term parent() {
		Term p=prior;
		while (p!=null) {
			if (p.child==this) return p;
			p=p.prior;
		}
		return null;
	}

	/**
	Iterate over children terms.
	<p>
	To use: <code> for(Term x: term) { ... }</code></p>
	*/
	public Terms iterator() { return new Terms(this); }
	
	// -- predicates........
	/**
	Test if term has a particular tag name.
	<p>
	This predicate can be used with any tag name String, but if the String
	is a "...literal..." Java String then it can be expressed as:
	<code> term.tag() == "tag"; </code></p>
	
	@param name   tag name string
	@return true if this term has this tag name, else false
	*/
	
	public boolean isTag(String name) {
		return tag==name.intern();
	}
	
	/**
	Test if term matched a particular string.
	@param str   text string
	@return true if this term matched this text str, else false
	*/
	public boolean isText(String str) {
		if (str.length()!=(eot-sot)) return false;
		return text.startsWith(str,sot);
	}
	
	// /**
	// Test if there is a child with given tag name.
	// @param tag   tag name string
	// @return true if this term has a child with this tag name, else false
	// */
	// public boolean hasChild(String tag) {
	// 	return (child(tag)!=null);
	// }
	// /**
	// Test for a child with a given tag name that matches a given string.
	// @param tag   tag name string
	// @param str   text string matched by tag
	// @return true if first child with tag name matched str, else false
	// */
	// public boolean hasChild(String tag, String str) {
	// 	Term kid=child(tag);
	// 	if (kid==null) return false;
	// 	return kid.isText(str);
	// }
	
	/**
	Test if there is a child with given tag name.
	@param tag   tag name string
	@return true if this term has a child with this tag name, else false
	*/
	public boolean has(String tag) {
		return (child(tag)!=null);
	}
	/**
	Test for a child with a given tag name that matches a given string.
	@param tag   tag name string
	@param str   text string matched by tag
	@return true if first child with tag name matched str, else false
	*/
	public boolean has(String tag, String str) {
		Term kid=child(tag);
		if (kid==null) return false;
		return kid.isText(str);
	}
	
	/**
	To check if term contains a fault message.
	<p>A fault tag is not a valid rule name: <tt>"-- fault report msg.... "</tt></p>
	@return true if the tag is a fault report, else false
	*/	
	public boolean isFault() { return tag.startsWith("--"); }

	/**
	get bag value previously assigned by an application
	<p>The bag is an Object slot available for applications to use.
		The parser does not read or write the value.</p>
	@return bag object, null if the bag has not been assigned
	*/
	public Object getBag() { return bag; }

	/**
	assign an application bag value
	<p>May be used to assign the result of a semantic method into a parse term.
		The parser does not read or write the value.</p>
	@param obj   value to assign to the terms bag
	*/
	public void putBag(Object obj) { bag=obj; }


	// tree print methods............................

	/**
	To display the parse tree, or a fault report.
	@return text string print representation for the parse tree structure.
	*/	
	public String toString() {
		StringBuilder sb=new StringBuilder();
		if (isFault()) faultDisplay(sb);
		else treeDisplay(sb,"\n","\t");
		return sb.toString();
	}

	void treeDisplay(StringBuilder s,String nl,String sp) {
		if (child==null) { // term
			s.append(tag);
			if (sot<=eot) {
				s.append(sp+"\"");
				chars(s,text,sot,eot);
				s.append("\"");
			}
		} else if (child.next==null) { // singleton
			s.append(tag+sp); // in-line
			child.treeDisplay(s,nl,sp);
		} else { // list of children...
			s.append(tag+nl+sp);
			child.treeDisplay(s,nl+sp,sp);
		}
		if (next!=null) { // next sibling...
			s.append(nl);
			next.treeDisplay(s,nl,sp);
		}
	}
	
	void faultDisplay(StringBuilder sb) {
		sb.append(tag);
		sb.append("\n");
		textCursor(sb,text,sot,eot);
		if (next!=null) next.faultDisplay(sb);
	}
	
	static void textCursor(StringBuilder sb ,String text,int sot,int eot) {
		//  pre....^....span....^...post..
		//  pre ...i
		if (eot<sot) return;
		int base=sb.length();
		if (sot<25) chars(sb,text,0,sot);
		else { sb.append(" ... "); chars(sb,text,sot-25,sot); }
		int i=sb.length();
		// i... span... j
		if (sot<eot) chars(sb,text,sot,eot);
		int j=sb.length();
		if (j>i+40) {
			sb.replace(i+16,j-16," ... ");
			j=sb.length();
		}
		// post...
		int k=eot+16, len=text.length();
		if (k+6>len) k=len;
		chars(sb,text,eot,k);
		if (k<len) sb.append(" ... ");
		// ruler...
		String blanks="                                             "; // >42
		sb.append("\n"+blanks.substring(0,i-base)+"^");
		if (j>i) sb.append(blanks.substring(0,j-i-1)+"^"); //  pre....^....span....^...post..
	}
	
	static void chars(StringBuilder s,String text,int i,int j) {
		for (int k=i;k<j;k++) {
			int x=text.charAt(k);
			if (x==92) s.append("\\");
			else if (x>31 && x<0XFF) s.append((char)x);
			else if (x==9) s.append("\\t");
			else if (x==10) s.append("\\n");
			else if (x==13) s.append("\\r");
			else s.append("\\u"+Integer.toHexString(x));
		}
	}
	

} // Term

class Terms implements java.util.Iterator<Term> {
	Terms(Term term) { this.term=term.child; }
	Term term;
	public boolean hasNext() { return term!=null; }
	public Term next() { if (term==null) return null; Term it=term; term=term.next; return it; }
	public void remove() { throw new UnsupportedOperationException("Terms.remove"); }
}
	

