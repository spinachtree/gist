
import org.spinachtree.gist.*;

import java.util.*;

public class Arith3 {
	
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
			System.out.println( in +" = "+ SemanticMethod.calc(tree) );
	}
	
	static enum SemanticMethod {
		Exp { // Exp   = Add / Sub / Term
			int arith(Term t) { return calc(t.child()); }
		},
		Add { // Add   = Exp '+' Term 
			int arith(Term t) { return calc(t.child("Exp")) + calc(t.child("Term")); }
		},
		Sub { // Sub   = Exp '-' Term 
			int arith(Term t) { return calc(t.child("Exp")) - calc(t.child("Term")); }
		},
		Term { // Term  = Mul / Div / Val
			int arith(Term t) { return calc(t.child()); }
		},
		Mul { // Mul   = Term '*' Val 
			int arith(Term t) { return calc(t.child("Term")) * calc(t.child("Val")); }
		},
		Div { // Div   = Term '/' Val 
			int arith(Term t) { return calc(t.child("Term")) / calc(t.child("Val")); }
		},
		Val { // Val   = int / '(' Exp ')'
			int arith(Term t) {
				Term x=t.child();
				if (x.isTag("int")) return Integer.parseInt(x.text());
				return calc(x); 
			}
		};

		static Map<String,SemanticMethod> methods=new HashMap<String,SemanticMethod>();

		static { for (SemanticMethod m: SemanticMethod.values()) { methods.put(m.toString(),m); } }

		static int calc(Term t) { 
			SemanticMethod m=methods.get(t.tag());
			if (m==null) throw new UnsupportedOperationException(t.toString());
			return m.arith(t);
		}
		
		abstract int arith(Term t);
	}

}
