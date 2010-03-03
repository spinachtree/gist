
import org.spinachtree.gist.*;

// Parser Expression Grammar style.
// - simple to read
// - operator precedence levels: op4 > op3 > op2 > op1
// - flat lists, left/right association delegated to evaluation

public class Arith4 {
	
	public static void main(String[] args) {

		// Li = Left association at priority level i...
		// Rj = Right association at priority level j..
			
		String arith=
		"R1    = var op1 L2                \n"+
		"L2    = L3 (op2 L3)*              \n"+
		"L3    = R4 (op3 R4)*              \n"+
		"R4    = op4? L5                   \n"+
		"L5    = val op5?                  \n"+
		"val   = var / int / '(' L2 ')'    \n"+
		"op1   : '='                       \n"+
		"op2   : '+' / '-'                 \n"+
		"op3   : '*' / '/' / '%'           \n"+
		"op4   : '++' / '--' / '+' / '-'   \n"+
		"op5   : '++' / '--'               \n"+
		"var   : 'a'..'z'+                 \n"+
		"int   : '0'..'9'+                 \n";

		Gist exp=new Gist(arith);
		
		String in="x=1+2+3*(3-4/2+1)-4";
		if (args.length>0) in=args[0];
		
		Term tree=exp.parse(in);
		
		System.out.println( in +" =>\n"+ tree );
		
		if (tree.isTag("R1")) // valid parse tree....
			System.out.println( in +" = "+ calc(tree.child("L2")) );
	}

	static int calc(Term t) { 
		if (t.isTag("int")) return Integer.parseInt(t.text());
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
	
}

