
import org.spinachtree.gist.*;

public class Arith1  {

	public static void main(String[] args) {

		String input="1+2+3*(3-4/2+1)-5";

		// Text-book style grammar with operator based parse tree.
		// -- requires left recursion
		// -- left association and operator precedence are explicit
		// -- app evaluation maps directly into the parse tree structure
		// -- more work for the parser, but very little benifit to the app.
		// -- I prefer the simplicity of the previous example

		Gist list = new Gist(
			"list = list '+' num | num \n"+
			"num  : '0'..'9'+ \n");

		Parse lst = list.parse("1+2+3");
		System.out.println(lst);

		// Gist exp = new Gist( // tiny test...
		// "exp   = add / int              \n"+
		// "add   = exp '+' int            \n"+
		// "int   : '0'..'9'+              \n");

		Gist exp = new Gist(
		"exp   = add | sub | term                \n"+
		"add   = exp '+' term                    \n"+
		"sub   = exp '-' term                    \n"+
		"term  = mul | div | val                 \n"+
		"mul   = term '*' val                    \n"+
		"div   = term '/' val                    \n"+
		"val   = int | '(' exp ')'               \n"+
		"int   : '0'..'9'+                       \n");

		Parse p = exp.parse(input);
		System.out.println(p);

		// Star rules prune the parse tree, print Term tree...
		// first choice / replaces longest choice | operator
		
		Gist exp1 = new Gist(
			"exp   = add / sub / term                \n"+
			"add   = exp '+' term                    \n"+
			"sub   = exp '-' term                    \n"+
			"term* = mul / div / val                 \n"+
			"mul   = term '*' val                    \n"+
			"div   = term '/' val                    \n"+
			"val*  = int / '(' exp ')'               \n"+
			"int   : '0'..'9'+                       \n");
		
		Term t = exp1.parse(input).termTree();
		System.out.println(t);
		
		// eval Term tree.....
		System.out.printf("%s = %s \n",input,eval(t));
	}

	// -- eval Term tree --------------------------------------

	//  term = {tag, text} | {tag, terms[]}

	static int eval(Term t) {
		if (t.tag=="int") return Integer.parseInt(t.text);
		int x=eval(t.terms[0]);
		if (t.terms.length==1) return x;
		int y=eval(t.terms[1]);
		if (t.tag=="add") return x+y;
		if (t.tag=="sub") return x-y;
		if (t.tag=="mul") return x*y;
		if (t.tag=="div") return x/y;
		System.out.println("woops, missed something: "+t.tag);
		return 0;
	}


}
