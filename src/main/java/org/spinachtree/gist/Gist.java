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

	static Parser gist;

	// static HashMap<String,Parser> library = new HashMap<String,Parser>();
	// 
	// static {
	// 	Map<String,String> grammars=Library.grammars;
	// 	for (String label: grammars.keySet()) {
	// 		Gist gist=new Gist(grammars.get(label));
	// 		library.put(label,gist.parser);
	// 	}
	// }

	String grammar; // source rules
	Parser parser; // parser for this grammar
	

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
//System.out.println("Gist====\n"+lines[0]);
		if (lines.length<1) throw new IllegalArgumentException("Missing grammar...");
		else if (lines.length==1) grammar=lines[0];
		else grammar=concat(lines);
		if (gist==null) gist=bootstrap();
//System.out.println("gist---------------------------"+gist);
		Term tree=gist.parse(grammar);
		if (!tree.isTag("rules")) throw new GistFault("Grammar rules fault:\n"+tree);
		parser=new Parser(Grammar.rules(tree));
	}
	
	Parser bootstrap() {
		Parser boot=new Parser(Boot.rules());
//System.out.println("boot---\n"+boot.rules.ruleNames.size());
		Term bootTree=boot.parse(Grammar.gistGrammar);
//System.out.println("bootTree--------------------------------------");
		Parser gistBoot=new Parser(Boot.rules(bootTree));
//System.out.println("gistBoot---\n"+gistBoot);
		Term gistTree=gistBoot.parse(Grammar.gistGrammar);
		return new Parser(Grammar.rules(gistTree));
	}

	// /**
	// Allows lines of grammar to be writen as list of strings.
	// @param lines   any number of grammar line string arguments.
	// @return a new Gist object
	// @throws	GistFault   an unchecked exception, subclass of RuntimeException,
	// reports grammar rule faults. Usually a faulty grammar is equivalent to
	// a source code error, but an application that allows grammar rules to be
	// edited may choose to catch GistFault exceptions.
	// */
	// public static Gist rules(String... lines) {
	// 	StringBuffer sb=new StringBuffer();
	// 	for (String line: lines) sb.append(line).append("\n");
	// 	return new Gist(sb.toString());
	// }
	
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
	public Term parse(String text) { return parser.parse(text); }

	
	// /**
	// assign the given label to this grammar
	// <p>Load this grammar into the library with the given label.
	// <p>This method can be used to label a local grammar with a global name.
	// 
	// @param label   grammar-name
	// @return Gist return this grammar
	// */
	// public Gist label(String label) {
	// 	library.put(label,this.parser);
	// 	return this;
	// }
	
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
	to inspect compiled rules

	@return  text for debug inspection
	*/
	public String inspect() {
		return parser.toString();
	}

	/**
	the source text of the grammar rules
	
	@return source grammar rules
	*/
	public String toString() {
		return grammar;
	}
	
	/**
	only needed for an application that wants to handle events
	@param action   an application sub-class implementing the Action i/f
	*/

	public Gist events(Action action) {
		parser.action=action;
		return this;
	}
	
//	static public String gistGrammar() { return Parser.gistGrammar; }

	// -- package internals -----------------------------------------------------------------

	Rule getRule(String name) { return parser.getRule(name); }

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
