package org.spinachtree.gist;

/**
 Only used if an application needs to respond to events.

<p>An object implementig the Action interface can be linked into a grammar
	 through the events method in the Gist class.</p>	
<p>An grammar rule event such as: <tt>&lt;name args&gt;</tt> will call
	 the event method with the rule name, event name and args.
	The Scan class provides methods to access the input text,
	cursor, parse tree and so forth.</p>
*/

public interface Action {

	boolean event(Scan scan, String rule, String event, String args);

}

