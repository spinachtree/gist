package org.spinachtree.gist;

import java.util.*;

/**
context for parse operators...

<p>The Parser contains the input text, cursor, parse tree term, etc.
Normally for private use internal to the parser, but an action
event handler is given access via public methods.
</p>

*/

class Parser {
	
	Parser(Grammar gist) {
		gist.buildParser(this);
	}
	
	Map<String,Rule> ruleMap=new HashMap<String,Rule>();

	Rule getRule(String name) { return ruleMap.get(name); }
	void putRule(String name, Rule rule) { ruleMap.put(name,rule); }
	
	Term fault=null;
	
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
	
	Action action=null; // event interface
	
//	Map<Rule,Memo> memos=new HashMap<Rule,Memo>();

	String start; // first rule name
	String trace;
	
	Rule startRule() {
		Rule rules=ruleMap.get("=");
		start=rules.name;
		return ruleMap.get(start);
	}

	Term parse(String src, String trace) {
		Rule rule=startRule(); //ruleMap.get("=");
		System.out.println("parse: "+rule.name+" trace="+trace);
		this.trace=trace;
		return parse(src);
	}
	
	Term parse(String src) {
		if (fault!=null) return fault;
		input=src;
		eot=src.length();
		pos=0;
		chr=src.codePointAt(pos);
		seed=new Term("<root>",src,pos,eot);
		tip=seed;	// tree growth tip
	//	Rule rule=ruleMap.get("=");
//System.out.println("parse rule="+(rule==null));
	//	start=rule.name;
		Rule rule=startRule();
		boolean result=rule.parse();
		if (!result) return faultResult(start+" parse failed... "); 
		if (pos<eot) return faultResult(start+" parse incomplete... "); 
		return seed.next;
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
	
	void reset(int p, Term t) {
		if (p>=eot) return;
		if (pos>peak) { peak=pos; top=tip;}
		tip=t;
		t.next=null;
		if (pos==p) return;
		pos=p;
		chr=input.codePointAt(pos);
	}
	

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

	public String toString() {
		String s="grammar:\n";
		Rule rules=ruleMap.get("=");
		for (String name: ((Rules)rules).ruleNames) s+=ruleMap.get(name).toString()+"\n";
		return s;
	}
	
/*	public String toString() {
		String s="grammar:\n";
		for (Rule rule: ruleMap.values()) s+=rule.toString()+"\n";
		return s;
	}
*/	
	String show(int i, int j) {
		String str="<"+i+".."+j+">";
		if (j<i || i<0 || j<0) return str;
		if ((j-i)<70) return str+input.substring(i,j);
		else return str+input.substring(i,i+20)+" ... "+input.substring(j-20,j);
	}
		
	// -- trace and fault reporting ------------------------------------

	Op fault(Rule rule, Term term, String msg) {
		// Rule: <name> <term.text>: msg
		return fault("Rule: "+rule.name+"  "+term.text()+"\n    "+msg);
	}

	Op fault(String msg) { 
		Term log=faultTerm(msg);
		return new False(this);
	}

	Term faultTerm(String msg) { 
		Term log=new Term("-- "+msg,null,0,-1);
		log.next=fault;
		fault=log;
		return log;
	}

	String traceReport(String msg) {
		if (peak<pos) peak=pos;
		Term term=new Term(report(msg),input,pos,peak);
		return term.toString();
	}

	String report(String msg) {
		return "-- "+msg+" ("+peak+" of "+eot+")"+trail();
	}

	String trail() {
		Term t=tip; // last rule term
		if (top!=null) t=top;
		List<String> tags=new ArrayList<String>();
		while (t!=null) { tags.add(t.tag()); t=t.prior; }
		if (tags.isEmpty()) return "";
		String report = "\n";
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

} // Parser

class GistFault extends RuntimeException {
	GistFault(String s) { super(s); }
}

/*
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
		return true;
	}
*/	
