
import org.spinachtree.gist.*;

public class Arith  {

	// Parser Expression Grammar style.
	// - simple to read, easy to parse
	// - operator precedence levels: op2 > op1
	// - flat lists, left association delegated to evaluation
	
	static final Gist exp = new Gist(
	"exp   = term (op1 term)*                \n"+
	"term  = val (op2 val)*                  \n"+
	"val   = int / '(' exp ')'               \n"+
	"op1   : '+' / '-'                       \n"+
	"op2   : '*' / '/'                       \n"+
	"int   : '0'..'9'+                       \n");

	public static void main(String[] args) {

		String input="1+2+3*(3-4/2+1)-3";
		if (args.length>0) input=args[0];
		
		System.out.println(exp);
		
		Parse parse = exp.parse(input);
		System.out.printf("%s => \n%s \n",input,parse);
		
		// eval Term tree.....
		System.out.printf("eval:  %s = %s \n",input,eval(parse.termTree()));
	}
	
	// -- eval Term tree --------------------------------------
	
	//  term = {tag, text} | {tag, terms[]}
	
	static int eval(Term t) {
		if (t.tag=="int") return Integer.parseInt(t.text);
		int val=eval(t.terms[0]);
		return reduce(val,1,t.terms);
	}
	
	static int reduce(int val, int i, Term[] ts) {
		if (i>ts.length-2) return val;
		String op=ts[i].text;
		int x=eval(ts[i+1]);
		if (op.equals("+")) val+=x;
		else if (op.equals("-")) val-=x;
		else if (op.equals("*")) val*=x;
		else if (op.equals("/")) val/=x;
		return reduce(val,i+2,ts);
	}

}
