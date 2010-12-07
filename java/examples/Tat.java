
import org.spinachtree.gist.*;

import java.net.*;
import java.io.*;
import java.util.*;

public class Tat  {

	static final Gist tatdoc=new Gist(
	"	tat   = item*					\n"+
	"	item  = head? (`neck body)? `foot		\n"+
	"	head  : graph text (nl graph text)*		\n"+
	"	neck  : vert* tab				\n"+
	"	body  = line (`vert+ `tab line)*		\n"+
	"	foot  : (!vert any)* vert*			\n"+
	"	text  : (sp|graph)*				\n"+
	"	line  : (tab|sp|graph)*				\n"+
	"	nl    : 10 / 13 10?				\n"+
	"	vert  : 10/13					\n"+
	"	tab   : 9					\n"+
	"	sp    : 32					\n"+
	"	graph : 33..1114111				\n"+
	"	any   : 1..1114111				\n");

	public static void main(String[] args) {
		// -demo -test files..... 
		boolean demo=true;
		for (String arg:args) {
			if (arg.equals("-t") || arg.equals("-test")) demo=false;
			else if (arg.equals("-d") || arg.equals("-demo")) demo=true;
			else runFile(findFile(arg),demo);
		}
	}

	static void runFile(File file, boolean demo) {
	
		System.out.println("--- "+file.getPath());
	
		Parse doc=tatdoc.parse(fileString(file));
	
		Gist gist=null;
	
		Span[] items=doc.spanTree().spans();
		for (Span item:items) { // item = head? body?
			Span[] parts=item.spans();
			if (parts.length==0) { //  blank line.......
				if (demo) System.out.print("\n");
			} //  prose para or inset block........
			else if (parts.length==1) { // head | body
				if (demo) System.out.println(doc.text(parts[0]));
			} else { //  item = head body
				if (demo) System.out.println(doc.text(item));
				String head=doc.text(parts[0]);
				String body=lines(doc,parts[1]);
				if (head.equals("gist")) gist=new Gist(body);
				else if (head.equals("parse")) doParse(gist,body,true,demo);
				else if (head.equals("fail"))  doParse(gist,body,false,demo);
			}
			if (demo) System.out.print("\n");
		}
	}
	
	static String lines(Parse doc, Span body) {
		// body  = line (`vert+ `tab line)*
		StringBuilder sb=new StringBuilder();
		Span[] spans=body.spans();
		for (Span line:spans) sb.append(doc.text(line)).append("\n");
		return sb.toString();
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

	// file access -------------------------------------------------------------------
	
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
