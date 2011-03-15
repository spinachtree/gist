
import org.spinachtree.gist.*;

import java.net.*;
import java.io.*;
import java.util.*;

// import Test

public class Tests  {
	
	// see Tat example..........

	static final String tatdoc=
	"	tat   = item*					\n"+
	"	item  = head? (neck body)? `foot		\n"+
	"	head  : graph text (nl graph text)*		\n"+
	"	neck  : vert* tab				\n"+
	"	body  : line (vert+ tab line)*			\n"+
	"	foot  : (!vert any)* vert*			\n"+
	"	text  : (sp|graph)*				\n"+
	"	line  : (tab|sp|graph)*				\n"+
	"	nl    : 10 | 13 10?				\n"+
	"	vert  : 10 | 13					\n"+
	"	tab   : 9					\n"+
	"	sp    : 32					\n"+
	"	graph : 33..1114111				\n"+
	"	any   : 1..1114111				\n";
	
	Gist tatGist;

	Tests() {  tatGist=new Gist(tatdoc,this); }
	
	void transform(String text) { tatGist.transform(text); }
	
	// transform methods --------------------------------------
	
	Test test=null;
	
	int testNum=0, runs=0, fails=0;

	public Object tat(Object[] xs) {
		results();
		System.out.printf("Test results: %d tests, %d runs, %d failed..... %n",testNum,runs,fails);
		return null;
	}

	public Object item(Object[] xs) {
		if (xs.length==1) // head
			prosePara((String)xs[0]);
		else if (xs.length==2) // neck body
			insetPara((String)xs[1]);
		else if (xs.length==3) { // head neck body
			String head=(String)xs[0];
			String body=(String)xs[2];
			tagText((String)xs[0],(String)xs[2]);
		}
		return null;
	}
	
	// tat methods --------------------------------------

	void prosePara(String text) { System.out.println(text); }
	
	void insetPara(String text) { System.out.println(text); }
	
	void tagText(String head, String body) {
		//System.out.println(head);
		if (head.equals("gist")) {
			results();
			test=new Test(body,testNum++);
		} else if (head.equals("grammar")) {
			System.out.println(test);
		} else if (head.equals("code")) {
			System.out.println(test.ruleCode());
		} else if (head.equals("parse")) {
			test.parse(body);
		} else if (head.equals("fail")) {
			test.fail(body);
		}
	}
	
	void results() {
		if (test==null) return;
		runs+=test.runs;
		fails+=test.fails;
	}
	
	// run test script -------------------------------------------------------------------
	
	void run(File file) {
		System.out.println("--- "+file.getPath());
		String text=readFile(file);
		// System.out.println(text);
		transform(text);
	}
	
	
	
	// file access -------------------------------------------------------------------
	
	static String readFile(File file) {
		StringBuilder sb=new StringBuilder();
		String ln;
		try {
			BufferedReader in=new BufferedReader(new FileReader(file));
			while ((ln=in.readLine())!=null) sb.append(ln).append("\n");
		} catch (IOException e) { System.out.println(e); System.exit(-1); }
		return sb.toString();
	}

	static void writeFile(File file,String text) {
		try {
			PrintWriter out= new PrintWriter(new FileWriter(file));
			out.print(text);
			out.close();
		} catch (IOException e) { System.out.println(e); System.exit(-1); }
	}

	static File findFile(String file) {
		// check for abs/rel file name... or url...  TODO
		return new File(System.getProperty("user.dir"),file);
	}

	// -- main -----------------------------------------------------------------------
	
	public static void main(String[] args) {
		Tests tests=new Tests();
		for (String arg:args) tests.run(findFile(arg));
	}
	
} // Tat
	

