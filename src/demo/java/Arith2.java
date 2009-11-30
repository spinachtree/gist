
import org.spinachtree.gist.*;

public class Arith2 {
	
	public static void main(String[] args) {
			
		String arith=
		"	Exp   = Add / Sub / Term    \n"+
		"	Add   = Exp '+' Term        \n"+
		"	Sub   = Exp '-' Term        \n"+
		"	Term  = Mul / Div / Val     \n"+
		"	Mul   = Term '*' Val        \n"+
		"	Div   = Term '/' Val        \n"+
		"	Val   = int / '(' Exp ')'   \n"+
		"	int   : '0'..'9'+       	\n";
			
		Gist exp=new Gist(arith);
		
		String in="1+2+3*(3-4/2+1)-4";
		if (args.length>0) in=args[0];
		
		Term tree=exp.parse(in);
		
		System.out.println( in +" =>\n"+ tree );
		
		if (tree.isTag("Exp")) // valid parse tree....
			System.out.println( in +" = "+ calc(tree) );
	}
	
	static int calc(Term t) {
		String tag=t.tag();
		if (tag=="int")
			return Integer.parseInt(t.text());
		if (tag=="Val") // Val   = int / '(' Exp ')'
			return calc(t.child());
		if (tag=="Add") // Add   = Exp '+' Term 
			return calc(t.child("Exp")) + calc(t.child("Term"));
		if (tag=="Sub") // Sub   = Exp '-' Term 
			return calc(t.child("Exp")) - calc(t.child("Term"));
		if (tag=="Term") // Term  = Mul / Div / Val
			return calc(t.child());
		if (tag=="Mul") // Mul   = Term '*' Val 
			return calc(t.child("Term")) * calc(t.child("Val"));
		if (tag=="Div") // Div   = Term '/' Val 
			return calc(t.child("Term")) / calc(t.child("Val"));
		if (tag=="Exp") // Exp   = Add / Sub / Term
			return calc(t.child());
		throw new UnsupportedOperationException(t.toString());
	}
	

}
