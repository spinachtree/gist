
import org.spinachtree.gist.*;

public class TwistedLR {

	// Example from PEG mail group
	// from: Peter Goodman
	
	// A <- A 'a'
	//   <- B
	// 
	// B <- B 'b'
	//   <- A
	//   <- C
	// 
	// C <- C 'c'
	//   <- B
	//   <- 'd'.

	static Gist twister = new Gist(
		" A = A 'a' / B         ",
		" B = B 'b' / A / C     ",
		" C = C 'c' / B / 'd'   ");

	// static Gist twister = new Gist(
	// 	" A = A a / B         ",
	// 	" B = B b / A / C     ",
	// 	" C = C c / B / d   ",
	// 	"a:'a'; b:'b'; c:'c'; d:'d' ");
	
	// Gist expansion....
	
	//  sub  = sub '-' int / int	-- left recursion
	//  sub* = int ('-' int)*	-- sub expanded
	
	// Exp   = Sub / int 
	// Sub   = Exp '-' int
	
	// Exp   = Exp '-' int / int
	// Exp*  = int ('-' int)*
	
	//"	Exp   = Add / Sub / Item    \n"+
	//"	Add   = Exp '+' Item        \n"+
	//"	Sub   = Exp '-' Item        \n"+
	//"	Item  = Mul / Div / Val     \n"+
	//"	Mul   = Item '*' Val        \n"+
	//"	Div   = Item '/' Val        \n"+
	//"	Val   = int / '(' Exp ')'   \n"+
	//"	int   : '0'..'9'+       	\n";
	
	// Exp = Add / Sub / Item
	// Exp = Exp '+' Item / Sub / Item
	// Exp* = (Sub/Item) ('+' Item)* / Sub / Item
	
	// A        = A a / B
	// A*       = B a*			-- A expanded
	// A*       = (B b / A / C) a*		-- B rule
	// A* B*    = (A / C) b* a*		-- B expanded
	// A* B*    = C b* a*			-- A pruned
	// A* B*    = (C c / B / d) b* a*	-- C rule
	// A* B*    = (C c / d) b* a*		-- B pruned
	// A* B* C* = (d c*) b* a*		-- C expanded
	
	// Dropping the quotes for readability (and ignoring the difference between
	// prioritized and ordered choice which doesn't matter here), we have:
	// 
	//  A <- A a | B
	//  B <- B b | A | C
	//  C <- C c | B | d
	// 
	// left-factoring A, B and C individually:
	// 
	//  A <- B a*
	//  B <- (A | C) b*
	//  C <- (B | d) c*
	// 
	// substituting for B in A and C:
	// 
	//  A <- (A | C) b* a*
	//  C <- ((A | C) b* | d) c*
	// 
	// expanding A:
	// 
	//  A <- A b* a* | C b* a*
	// 
	// left-factoring A and simplifying:
	// 
	//  A <- C b* a* (b* a*)*
	// 
	//  A <- C (a | b)*
	// 
	// therefore:
	// 
	//  A | C = C (a | b)* | C
	//        = C (a | b)*
	// 
	// substituting A | C into C:
	// 
	//  C <- (C (a | b)* b* | d) c*
	// 
	//  C <- (C (a | b)* | d) c*
	// 
	// expanding C:
	// 
	//  C <- C (a | b)* c* | d c*
	// 
	// left-factoring C and simplifying:
	// 
	//  C <- d c* ((a | b)* c*)*
	// 
	//  C <- d c* (a | b | c)*
	// 
	//  C <- d (a | b | c)*

	public static void main(String[] args) {

		String in="dcccbbaaa";
		if (args.length>0) in=args[0];

		Term tree=twister.parse(in);

		System.out.println( in +" =>\n"+ tree );

		in="dcccbbaaa";
		tree=twister.parse(in);
		System.out.println( in +" =>\n"+ tree );

		in="d";
		tree=twister.parse(in);
		System.out.println( in +" =>\n"+ tree );

		in="dc";
		tree=twister.parse(in);
		System.out.println( in +" =>\n"+ tree );

		in="da";
		tree=twister.parse(in);
		System.out.println( in +" =>\n"+ tree );

		in="dba";
		tree=twister.parse(in);
		System.out.println( in +" =>\n"+ tree );

		in="dab";
		tree=twister.parse(in);
		System.out.println( in +" =>\n"+ tree );

	}
}

