package org.spinachtree.gist;

import java.util.*;

/**
 Vent provides an interface for events...

<p>A Vent sub-class can be linked into a grammar through
	the events method in the Gist class.</p>	
<p>An application can extend the Vent class with a subclass that
	implements the event method. An event such as
	<tt><name args></tt> in a grammar will call the event
	method with the rule name, event name and args.
	The Vent class provides methods to access the input scan,
	the parse tree and so forth.</p>
*/

public class Vent {

	Scan scan;
	
	boolean event(Scan scan, String rule, String event, String args) {
		this.scan=scan;
		return event(rule,event,args);
	}
	
	// public interface methods...............
	
	/**
	event interface
	<p>An event such as <tt><name args></tt> in a grammar will call the event
	method with the rule name, event name and args.

	@param rule   name of the rule containing this event
	@param event   name of the event, <name ...>
	@param args   string following event name, <name args>
	@return fail or continue the parse
	*/
	public boolean event(String rule, String event, String args) {
		System.out.println(rule+": <"+event+" "+args+">");
		return true;
	}
	
	/**
	position of cursor index in the input scan
	<p>This index may not be a character index. The character encoding
		may use a byte index, and even in a Java string the use
		of surrogate pairs the index is not one-to-one with chars.

	@return cursor position
	*/
	public int pos() { return scan.pos; }
	
	/**
	last term created
	<p>Terms prior to the event, but the parent host rule will
		not yet exist (nor any other incomplete parents).

	@return last term
	*/
	public Term tip() { return scan.tip; }

	/**
	current char code
	<p>char at current cursor position

	@return int char point code
	*/
	public int codePoint() { return scan.codePoint(); }

	/**
	advance to the next character position
	<p>cursor position will increase by one or more

	*/
	public void advance() { scan.advance(); }
	
}
