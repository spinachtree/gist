package org.spinachtree.gist;

import java.util.*;


/**
 Gist generates a parser for a grammar.
 <p>
	For Gist documentation see: http://spinachtree.org/gist
	</p>
	
 <p>A simple example of application useage:</p>
<pre style="background-color: #fffdda">

import org.spinachtree.gist.*;

public class Date {
	static Gist date = new Gist(
	    "Date  = year '-' month '-' day ",
	    "year  : d d d d ",
	    "month : d d? ",
	    "day   : d d? ",
	    "d     : '0'..'9' ");
	public static void main(String[] args) {
		Term dt=date.parse("2009-8-7");
		System.out.println(dt);
	}
}
</pre>
Running this example will print the parse tree:
<pre style="background-color: #fffdda">
Date
    year    "2009"
    month   "8"
    day     "7"
</pre>
<p>
	An application can navigate and access text matched by terms in the parse tree,
	for example: <code>dt.text("month") == "8"</code> , see: Term class.</p>

*/

public class Gist {

	static Rules gist;

	String grammar; // source rules
	Rules rules; // Op rules for this grammar

	/**
	The basic constructor takes grammar rules.
	<p>The grammar can be given as one or more text string.
	The strings are concatenated together with a new-line separator. 
	</p>
	
	@throws	GistFault   an unchecked exception, subclass of RuntimeException,
	reports grammar rule faults. Usually a faulty grammar is equivalent to
	a source code error, but an application that allows grammar rules to be
	edited may choose to catch GistFault exceptions.
	*/
	public Gist(String... lines) {
		if (lines.length<1) throw new IllegalArgumentException("Missing grammar...");
		else if (lines.length==1) grammar=lines[0];
		else grammar=concat(lines);
		if (gist==null) { gist=bootstrap(); }
		Term tree=gist.parse(grammar);
		if (!tree.isTag("rules")) throw new GistFault("Grammar rules fault:\n"+tree);
		rules=Grammar.rules(tree);
	}
	
	Rules bootstrap() {
		Rules boot=Boot.rules();
		Term bootTree=boot.parse(Grammar.gistGrammar);
		Rules gistBoot=Boot.rules(bootTree);
		Term gistTree=gistBoot.parse(Grammar.gistGrammar);
		return Grammar.rules(gistTree);
	}

	/**
	Generate a parse tree for the input text.
	<p>A parse fault (syntax failure, or an incomplete parse) will
		generate a fault report as the root term of the parse tree.</p>

	<p>The standard way to test for a successful parse is to check that
		the root term has a tag name corresponding to the grammar start rule name.</p>
		
	<p>The parse tree <code>toString()</code> method generates text to
		display the tree or a fault report.</p>
		
	@param text   the input string to be parsed
	@return root term of the parse tree, or a fault report
	*/
	public Term parse(String text) { return rules.parse(text); }
	
	/**
	only needed for an application that wants to handle events
	@param action   an application sub-class implementing the Action i/f
	*/

	public Gist events(Action action) {
		rules.action=action;
		return this;
	}
	
	/**
	load a grammar into the library
	<p>A grammar must be loaded into the library for
		an external reference in another grammar
		to be able to find it.
	@param label   grammar name, may use Java package name format
	*/
	public static void load(String label, String... lines) {
		Library.put(label,concat(lines));
	}

	/**
	put this grammar into the library under the given label
	<p>A grammar must be loaded into the library for
		an external reference in another grammar
		to be able to find it.
	
	@param label   grammar name, may use Java package name format
	@return Gist return this grammar
	*/
	public Gist label(String label) {
		Library.put(label,this);
		return this;
	}

	/**
	to inspect compiled rules

	@return  text for debug inspection
	*/
	public String inspect() {
		return rules.toString();
	}

	/**
	the source text of the grammar rules
	
	@return source grammar rules
	*/
	public String toString() {
		return grammar;
	}
	
	// -- package internals -----------------------------------------------------------------

	Rule getRule(String name) { return rules.getRule(name); }

	static String concat(String[] lines) {
		StringBuffer sb=new StringBuffer();
		for (String line: lines) sb.append(line).append("\n");
		return sb.toString();
	}

	public static void main(String args[]) {
		System.out.println("Gist package: "+
			Gist.class.getPackage().getImplementationVersion()+
			"\nimport org.spinachtree.gist.*;"+
			"\nfor documentation see: http://spinachtree.org/gist");
		System.exit(0);
	}

} // Gist
