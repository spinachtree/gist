
package org.spinachtree.gist;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 Create a parser for a grammar.

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
		Parse parse=date.parse("2009-8-7");
		System.out.println(parse);
	}
}
</pre>
Running this example will print the syntax tree:
<pre style="background-color: #fffdda">
Date
    year    "2009"
    month   "8"
    day     "7"
</pre>
<p>
The Parse result for a sucsessful match can be verified with isOK().
A simple Java Term tree is available with:
<pre style="background-color: #fffdda">
	Term dt=parse.termTree();
</pre>
<p>
An application can navigate and access text matched by terms in the syntax tree,
for example:
</p>
<pre style="background-color: #fffdda">
	dt.child("month").text.equals("8")
</pre>
<p>see: Term class.</p>

*/

public class Gist {
	 	
	static final String testGrammar = 
	"rules = (rule/s)*\n"+
	"rule  = name s defn s sel\n"+
	"sel   = seq (s 47 s seq)*\n"+
	"seq   = rep (32+ rep)*\n"+
	"rep   = (ref/code/group) repn?\n"+
	"group = 40 s sel s 41 \n"+
	"code  = int (46 46 int)?\n"+
	"ref   = name\n"+
	"name  : (65..90/97..122)+\n"+
	"int   : 48..57+\n"+
	"repn  : 42..43/63 \n"+
	"defn  : 61/58  \n"+
	"s     : 9..32*\n";

	static final String bootGrammar = 
	"rules   = (rule/s)*                  \n"+
	"rule    = head s sel                 \n"+
	"head    = name star? s defn          \n"+
	"sel     = seq (or seq)*              \n"+
	"seq     = rep (and rep)*             \n"+
	"rep     = item repn?                 \n"+
	"item    = ref/code/quote/group       \n"+
	"group   = '(' s sel s ')'            \n"+
	"code    = int ('.' '.' int)?         \n"+
	"quote   = quo ('.' '.' quo)?         \n"+
	"ref     = elide? name                \n"+
	"quo     : 39 (32..38/40..126)* 39    \n"+
	"name    : ('A'..'Z'/'a'..'z')+       \n"+
	"int     : '0'..'9'+                  \n"+
	"repn    : '*'/'+'/'?'                \n"+
	"defn    : '='/':'                    \n"+
	"star    : '*'                        \n"+
	"elide   : '`'                        \n"+
	"or      : s '/' s                    \n"+
	"and     : (9/32)+                    \n"+
	"s       : 9..32*                     \n";
	
	// Gist grammar expressed as a boot grammmar:
	// - only PEG x/y priority choice, not x|y longest choice
	// - no hex numeric char codes 0x20
	// - no multi-line sequences (multi-line choices are ok)
	// - can't use {many} or [optional] syntax
	// - no x^y, but-not syntax
	// - no comments

	static final String gistGrammar = 
	"rules   = (rule / `w)*	    				\n"+
	"rule    = head `w alt	                		\n"+
	"head    = name star? `w defn				\n"+
	"alt*    = sel (`w '|' `w sel)*				\n"+
	"sel*    = seq (`w '/' `w seq)*				\n"+
	"seq*    = rep (`h (',' `w)? rep)*			\n"+
	"rep*    = elem repn? / prime				\n"+
	"elem*   = item (`w '^' `w item)*			\n"+
	"item*   = ref/quote/code/group     			\n"+
	"prime*  = many/option/not/isa/pre  			\n"+
	"group*  = '(' `w alt `w ')'				\n"+
	"many    = '{' `w alt `w '}'				\n"+
	"option  = '[' `w alt `w ']'				\n"+
	"not     = '!' `h rep					\n"+
	"isa     = '&' `h rep					\n"+ 
	"pre     = '@' pat/pec/pan              		\n"+
	"pat     = eq? `h ref					\n"+
	"pec     = ':' `h item					\n"+ 
	"pan     = '&' `h item					\n"+ 
	"quote   = quo ('..' quo)?				\n"+ 
	"code    = '0' ('x'/'X') hexs / ints			\n"+
	"ints    = int ('..' int)?				\n"+
	"hexs    = hex ('..' hex)?				\n"+
	"ref     = elide? name ('.' name)*			\n"+
	"name    : alpha alnum*					\n"+
	"defn    : '=' / ':'					\n"+
	"star    : '*'						\n"+
	"repn    : '+'/'?'/'*'					\n"+
	"int     : digit+					\n"+
	"hex     : (digit/'a'..'f'/'A'..'F')+			\n"+
	"quo     : 39 (32..38/40..126)* 39			\n"+
	"alnum   : alpha/digit					\n"+
	"alpha   : 'a'..'z'/'A'..'Z'				\n"+
	"digit   : '0'..'9'					\n"+
	"eq      : '='						\n"+
	"elide   : '`'						\n"+
	"blank   : 9/32						\n"+
	"print   : 9/32..1114111				\n"+
	"space   : 9..13/32					\n"+
	"comment : ('--'/'//'/'#') print*			\n"+
	"h       : blank*					\n"+ 
	"s       : space*					\n"+ 
	"w       : (s comment?)*				\n"; 

	static Parser bootParser;
	static Parser gistParser;

	String grammar; // input grammar
	Parser parser;  // parser for this grammar

	// public static Gist lines(String...lns) {
	// 	String src="";
	// 	for (String ln:lns) src+=ln+"\n";
	// 	return new Gist(src);
	// }

	/**
	The basic constructor takes grammar rules as a String.
	
	*/
	public Gist(String grammar) {
		this.grammar=grammar;
		if (gistParser==null) bootstrap();
		parser=Compiler.compile(grammar,gistParser);
	}

	/**
	Constructor for a grammar with external references.
	*/
	public Gist(String grammar, Map<String,Gist> glib, List<String> defaults) {
		this.grammar=grammar;
		if (gistParser==null) bootstrap();
		parser=Compiler.compile(grammar,gistParser,glib,defaults);
	}
	
	/**
	Parse an input string and generate a syntax tree.
	*/
	public Parse parse(String src) {
		return parser.parse(src);
	}
	
	void bootstrap() {
		bootParser=Compiler.compile(Boot.bootGrammar,Boot.parser());
		gistParser=Compiler.compile(gistGrammar,bootParser);
	}

	/**
	The first rule name of the grammar.
	*/
	public String startName() { return parser.tagName(0); }

	// public int tag(String name) { return parser.tag(name); }
	void dump() { CodePad.dump(parser); }
	
	/**
	The grammar text string.
	*/
	public String toString() { return grammar; }
	
}