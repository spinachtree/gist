import org.spinachtree.gist.*;

// Text-book style grammar with operator based parse tree.
// - requires left recursion
// - evaluation maps directly into parse tree structure
// - left association and operator precedence are explicit


class ArithAct implements org.spinachtree.gist.Action {
	
	public boolean event(Parser par,String rule,String event,String args) {
		System.out.println("Event --> "+event);
		return true;
	}
	
}

public class Arithe {
	
	public static void main(String[] args) {
			
		String arith=
		"Exp   = Add / Sub / Term         \n"+
		"Add   = Exp '+' Term   <add>     \n"+
		"Sub   = Exp '-' Term   <sub>     \n"+
		"Term  = Mul / Div / Val          \n"+
		"Mul   = Term '*' Val   <mul>     \n"+
		"Div   = Term '/' Val   <div>     \n"+
		"Val   = int / '(' Exp ')'        \n"+
		"int   : '0'..'9'+                \n";
		
		Gist exp=new Gist(arith);
		
		ArithAct actor = new ArithAct();

		//exp.events(((org.spinachtree.gist.Action) actor));
		exp.events(actor);
		
		String in="1+2+3*(3-4/2+1)-4";
		if (args.length>0) in=args[0];
		
		Term tree=exp.parse(in);
		
		System.out.println( in +" =>\n"+ tree );
		
		if (tree.isTag("Exp")) // valid parse tree....
			System.out.println( in +" = "+ calc(tree) );
	}
		
	static int calc(Term t) {
		if (t.tag()=="int") return Integer.parseInt(t.text());
		Term x=t.child(); // t = x y
		Term y=x.next();
		int i=calc(x);
		if (y==null) return i; // Exp|Term|Val
		int j=calc(y);  // t => x y
		String tag=t.tag();
		if (tag=="Add") return i+j;
		if (tag=="Sub") return i-j;
		if (tag=="Mul") return i*j;
		if (tag=="Div") return i/j;
		throw new UnsupportedOperationException(t.toString());
	}

}
