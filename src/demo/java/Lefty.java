
import org.spinachtree.gist.*;

public class Lefty {
	
	public static void main(String[] args) {
		
		System.out.println("Gist TestLeft===============================================================TestLeft");
		
		String leftTest=
		"	Sub   = Sub '-' int / int  \n"+
		"	int   : '0'..'9'+   \n";
		
		Gist left=new Gist(leftTest);
		System.out.println(leftTest);
		System.out.println(left.inspect());
		Term sub=left.parse("5-3-2");
		System.out.println(sub);
			
		String exp1Test=
		"	Exp   = Sub / int   \n"+
		"	Sub   = Exp '-' int   \n"+
		"	int   : '0'..'9'+   \n";
		
		Gist exp1=new Gist(exp1Test);
		System.out.println(exp1Test);
		System.out.println(exp1.inspect());
		Term ex1=exp1.parse("5-3-2");
		System.out.println(ex1);
			
		System.out.println("=========================================================================Exp");
		
		String expTest=
		"	Exp   = Add / Sub / Item    \n"+
		"	Add   = Exp '+' Item        \n"+
		"	Sub   = Exp '-' Item        \n"+
		"	Item  = Mul / Div / Val     \n"+
		"	Mul   = Item '*' Val        \n"+
		"	Div   = Item '/' Val        \n"+
		"	Val   = int / '(' Exp ')'   \n"+
		"	int   : '0'..'9'+       	\n";
			
		Gist exp=new Gist(expTest);
		System.out.println(expTest);
		System.out.println(exp.inspect());
		Term ex=exp.parse("1+2*(3-4/2+1)");
		System.out.println(ex);
		
		
		System.out.println("=========================================================================Ambiguous");
		
		String ambigTest=
		"	Exp   = Add / Sub / Item    \n"+
		"	Add   = Exp '+' Exp        \n"+
		"	Sub   = Exp '-' Exp        \n"+
		"	Item  = Mul / Div / Val     \n"+
		"	Mul   = Item '*' Item        \n"+
		"	Div   = Item '/' Item        \n"+
		"	Val   = int / '(' Exp ')'   \n"+
		"	int   : '0'..'9'+       	\n";
			
		Gist ambig=new Gist(ambigTest);
		System.out.println(ambigTest);
		System.out.println(ambig.inspect());
		Term am=ambig.parse("(1+2)+3");
		System.out.println(am.text());
		System.out.println(am);
		am=ambig.parse("1+2*(3-4/2+1)-9");
		System.out.println(am.text());
		System.out.println(am);
		
	}
	
}
