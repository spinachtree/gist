
package org.spinachtree.gist;

import java.net.*;
import java.io.*;
import java.util.*;

/**
Gist library file loader.

<p>
Glib can load grammar(s) from a file, reading rules inset under a "gist" tag. A set of grammars loaded in this way
is available as a library of Gist parsers identified by their first rule name.
</p><p>
Examples of inputs for a grammar to parse may follow the "gist" grammar rules, inset under a "parse" (or "fail") tag.
Glib can be run from the command line to parse the input examples. They can be run with -demo option to display the
input file and the parser syntax tree resuts, or with a -test option to run the input examples without any output display
unless an unexpected result is found (ie a "parse" tag input example fails).
</p>
<pre style="background-color: #fffdda">
title	Example grammar file.

Uses "gist" to tag grammar rules, and "parse" to tag input sample.

gist
	tat   = (item? `nl?)*
	item  = head? body?
	head  : char+ (nl char+)*
	body  = args? box? 
	args  = (`tab+ arg)+
	arg   : char*
	box   = (`nl? `tab line)+
	line  : (char|tab)*
	tab   : 9
	nl    : 10 / 13 10?
	char  : 32..1114111

parse
	title	A Little example

	First a little para
	of prose...

	book
		title	A Good Book.
		author
			name	A Author
			email	author@office
		isbn	123456789
</pre>

*/

public class Glib {

	static final Gist tatdoc=new Gist(
	"	tat   = (item? `nl?)*           \n"+
	"	item  = head? body?            \n"+
	"	head  : char+ (nl char+)*      \n"+
	"	body  = args? box?             \n"+
	"	args  = (`tab+ arg)+           \n"+
	"	arg   : char*                  \n"+
	"	box   = (`nl? `tab line)+      \n"+
	"	line  : (char|tab)*            \n"+
	"	tab   : 9                      \n"+
	"	nl    : 10 / 13 10?            \n"+
	"	char  : 32..1114111            \n");


	/**
	Create a library of grammars from source files (gist tag).
	*/
	public Glib(String... files) {
		for (String fn:files) loadFile(fn);
	}

	Map<String,Gist> lib=new HashMap<String,Gist>();
	List<String> libNames=new ArrayList<String>();
	
	/**
	Load (more) grammars from source files (gist tag).
	*/
	public void loadFile(String fn) {
		File file=findFile(fn);
		Parse doc=tatdoc.parse(fileString(file));
		Span[] items=doc.span.spans();
		for (Span item:items) {
			Span[] parts=item.spans(); // head body
			if (parts.length==2 && doc.text(parts[0]).equals("gist"))
				loadGist(doc.text(parts[1]));
		}
	}

	/**
	Load a grammar into Glib library.
	*/
	public Glib loadGist(String grammar) {
		Gist gist= new Gist(grammar,lib,libNames);
		String start=gist.startName();
		if (get(start)==null) { libNames.add(start); }
		lib.put(start,gist);
		return this;
	 }
	
	/**
	Get a named grammar from the Glib library.
	*/
	public Gist get(String name) { return lib.get(name);  }
		
	/**
	Short for: get(name).parse(input);
	*/
	public Parse parse(String name, String input) { return get(name).parse(input); }

	/**
	Print the file showing results from gist parse examples.
	*/
	public static void demo(File file) { runFile(file,true); }

	/**
	Run the file gist parse examples, and report any faults.
	*/
	public static void test(File file) { runFile(file,false); }
	
	/**
	Command line access to -demo or -test grammar files.
	
	eg> java -cp gist.jar Glib -demo file
	*/
	public static void main(String[] args) {
		// -demo -test files..... 
		boolean demo=true;
		for (String arg:args) {
			if (arg.equals("-t") || arg.equals("-test")) demo=false;
			else if (arg.equals("-d") || arg.equals("-demo")) demo=true;
			else runFile(findFile(arg),demo);
		}
	}

	// --- run demo/test, tat file tags: gist, parse, etc..  -demo, -test, ..................................
	
	static void runFile(File file, boolean demo) {
		
		System.out.println("--- "+file.getPath());
		//System.out.println("length: "+file.length());
			
		Parse doc=tatdoc.parse(fileString(file));
		//System.out.println(doc);
		
		Gist gist=null;

		Span[] items=doc.span.spans();
		for (Span item:items) {
			Span[] parts=item.spans();
			//  blank line.......
			if (parts.length==0) {
				if (demo) System.out.print("\n");
			//  prose para or inset block........
			} else if (parts.length==1) { // head? body?
				if (demo)  System.out.println(doc.text(parts[0]));
			//  head body.....
			} else {
				if (demo) System.out.println(doc.text(item));
				String label=doc.text(parts[0]);
				String flds=fields(doc,parts[1]);
				if (label.equals("gist"))
					gist=doGist(doc.text(parts[1]));
				else if (label.equals("parse")) 
					doParse(gist,flds,true,demo);
				else if (label.equals("fail")) 
					doParse(gist,flds,false,demo);
				else if (label.equals("dump")){ 
					gist.dump();}
			}
			if (demo) System.out.print("\n");
		}
	}
	
	static Gist doGist(String rules) {
		return new Gist(rules);
	}
	
	static void doParse(Gist gist, String text, boolean expected, boolean demo) {
		if (gist==null) {
			System.out.println("***missing gist grammar rules.....");
			return;
		}
		Parse gp=gist.parse(text);
		if (gp.isOK()!=expected) fault(gp,demo);
		else if (demo) System.out.print(gp);
	}
	
	static void fault(Parse gp, boolean demo) {
		System.out.println("%< --- unexpected -----------");
		System.out.println(gp);
		System.out.println("--- unexpected ----------- >%");
		if (!demo) System.exit(-1);
	}
	
	static String fields(Parse doc,Span body) {
		// body  = args? box? 
		// args  = (`tab+ arg)+
		// box   = (`nl? `tab line)+
		StringBuilder sb=new StringBuilder();
		Span args=body.span(doc.tagId("args"));
		if (args!=null) {
			Span[] argn=args.spans();
			sb.append(doc.text(argn[0]));
			for (int i=1;i<argn.length;i++) {
				if (i>0) sb.append("\t");
				sb.append(doc.text(argn[i]));
			}
		}
		Span box=body.span(doc.tagId("box"));
		if (box==null) return sb.toString();
		Span[] lines=box.spans();
		for (int i=0;i<lines.length;i++) {
			if (i>0) sb.append("\n");
			sb.append(doc.text(lines[i]));
		}
		return sb.toString();
	}

	static String fileString(File file) {
		StringBuilder sb=new StringBuilder();
		String ln;
		try {
			BufferedReader in=new BufferedReader(new FileReader(file));
			while ((ln=in.readLine())!=null) sb.append(ln).append("\n");
		} catch (IOException e) { System.out.println(e); System.exit(-1); }
		return sb.toString();
	}
	
	static File findFile(String file) {
		// check for abs/rel file name... or url...  TODO
		return new File(System.getProperty("user.dir"),file);
	}
	
}