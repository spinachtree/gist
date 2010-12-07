
package org.spinachtree.gist;

import java.util.*;

/**
Term nodes represent the syntax tree.

<p>
A syntax tree of Term nodes can be created from the low level Span tree the parser machine generates.
Term objects provide a simple Java data structure for easy access by applications.
</p>
<pre>
	{String tag, Term[] terms, String text}
		tag = rule name
		terms = children terms, null in leaf terminal nodes
		text = leaf node matched substring of input, null in interior nodes
</pre>
*/

public class Term {
	
	/**
 	Creates a leaf node, a terminal text match term....
	*/
	public Term(String tag, String text) {
		this.tag=tag.intern();
		this.text=text;
		terms=null;
	}
	
	/**
 	Creates an interior node, a term with children terms....
	*/
	public Term(String tag, Term[] terms) {
		this.tag=tag.intern();
		this.terms=terms;
		text=null;
	}
	
	/**
	Name of rule that generated this term.
	*/
	public String tag;
	/**
	Text matched if this is a leaf term, null otherwise.
	*/
	public String text;
	/**
	Array of children terms.
	*/
	public Term[] terms;

	/**
	first child term
	*/
	public Term first() { return terms[0]; }

	/**
	last child term.
	*/
	public Term last() { return terms[terms.length-1]; }

	/*
	alias: first child.
	*/
	public Term term() { return first(); }
	
	public Term term(String target) {
		if (terms==null) return null;
		for (Term t:terms)
			if (t.tag.equals(target)) return t;
		return null;
	}

	/**
	array of children terms, null in a leaf term.
	*/
	public Term[] terms()  { return terms; }
	
	/**
	arry of children terms with the given target name.
	*/
	public Term[] terms(String target)  {
		if (terms==null) return null;
		ArrayList<Term> ts=new ArrayList<Term>();
		for (Term t:terms) if (t.tag.equals(target)) ts.add(t);
		return ts.toArray(new Term[ts.size()]);
	}
	/**
	alias: first term
	*/
	public Term child() { return first(); }
	
	/**
	alias: term(target), first target child.
	*/
	public Term child(String target) { return term(target); }

	/**
	array of children terms, null in a leaf node.
	*/
	public Term[] children()  { return terms; }
	
	/**
	array of children with given taget tag name.
	*/
	public Term[] children(String target) { return terms(target); }
	
	/**
	count of terms in tree.
	*/
	public int count() {
		if (terms==null) return 1;
		int n=1;
		for (Term kid:terms) n+=kid.count();
		return n;
	}

	// -- Term tree toString ------------------------------------------------------------------
	
	public String toString() {
		StringBuilder sb=new StringBuilder();
		tree("",sb);
		return sb.toString();
	}
	
	void tree(String inset, StringBuilder sb) {
		if (terms==null) { // leaf terminal
			sb.append(inset).append(tag).append(" ");
			quote(sb,text);
			sb.append("\n");
		} else {
			sb.append(inset).append(tag).append(" ");
			Term[] kids=terms;
			while (kids!=null && kids.length==1) {
				Term kid=kids[0];
				sb.append(kid.tag).append(" ");
				if (kid.terms==null) {
					quote(sb,kid.text);
					kids=null;
				} else kids=kid.terms;
			}
			sb.append("\n");
			if (kids!=null)
				for (Term kid: kids) kid.tree(inset+"    ",sb);
		}
	}
	
	void quote(StringBuilder sb, String txt) {
		sb.append("\"");
		for (int k=0;k<txt.length();k++) {
			int x=txt.charAt(k);
			if (x==92) sb.append("\\");
			else if (x==34) sb.append("\\\"");
			else if (x>31 && x<0XFF) sb.append((char)x);
			else if (x==9) sb.append("\\t");
			else if (x==10) sb.append("\\n");
			else if (x==13) sb.append("\\r");
			else sb.append("\\u"+Integer.toHexString(x));
		}
		sb.append("\"");
	}
	
}