
import org.spinachtree.gist.*;

public class ArithEx1 {

	// Parser Expression Grammar style.
	// - simple to read
	// - operator precedence levels: op2 > op1
	// - flat lists, left association delegated to evaluation

	static Gist exp = new Gist(
		"Exp   = Mul (sum Mul)*",
		"Mul   = Val (mul Val)*",
		"Val.. = int / '(' Exp ')'",
		"sum   : '+' / '-'",
		"mul   : '*' / '/'",
		"int   : '0'..'9'+");

	static int calc(Term t) { 
		if (t.tag()=="int") return Integer.parseInt(t.text());
		Term x=t.child(); // t = x (op y)*
		return reduceLeft(calc(x), x.next());
	}

	static int reduceLeft(int i, Term op) {
		// i (op y)*
		if (op==null) return i;
		Term y=op.next();
		int j=calc(y);
		if      (op.isText("+")) i+=j;
		else if (op.isText("-")) i-=j;
		else if (op.isText("*")) i*=j;
		else if (op.isText("/")) i/=j;
		else throw new UnsupportedOperationException(op.text());
		return reduceLeft(i, y.next());
	}	

	public static void main(String[] args) {
		
		String in= "1+2*(3-6/3+1)"; // "1+2+3*(3-4/2+1)-4";
		if (args.length>0) in=args[0];
	
		Term tree=exp.parse(in);
	
		System.out.println( in +" =>\n"+ tree );
	
		if (tree.isTag("Exp")) // valid parse tree....
			System.out.println( in +" = "+ calc(tree) );
	}
}

