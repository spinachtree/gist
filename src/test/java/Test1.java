
import org.spinachtree.gist.*;

public class Test1 {
	
	public static void main(String[] args) {
		
		System.out.println("Gist Test1===============================================================Test1");
		
		String dateGrammar=
		"	Date  = year '-' month '-' day	\n"+
		"	year  : d d d d					\n"+
		"	month : d d?					\n"+
		"	day   : d d?					\n"+
		"	d     : '0'..'9'				\n";
		
		Gist dateGist=new Gist(dateGrammar);
		System.out.println(dateGist);
		System.out.println(dateGist.inspect());
		Term date=dateGist.parse("2001-2-3");
		System.out.println(date);
		//Gist gistGist=new Gist(Gist.gistGrammar);
		//System.out.println(gistGist.parser);
		
		String charsTest=
		"	Nums = u l e         \n"+
		"	n : '0'..'9'        \n"+
		"	u : !('0'..'5')	n   \n"+
		"	l : !%53..126 n		\n"+
		"	e : !'1'!'3'!('5'/'7'/'9')	n	\n";
		
		Gist nums=new Gist(charsTest);
		System.out.println(charsTest);
		System.out.println(nums.inspect());
		Term ns=nums.parse("702");
		System.out.println(ns);
		
		String leftTest=
		"	Sub   = Sub '-' int / int  \n"+
		"	int   : %digit+   \n";
		
		Gist left=new Gist(leftTest);
		System.out.println(leftTest);
		System.out.println(left.inspect());
		Term sub=left.parse("5-3-2");
		System.out.println(sub);
			
		String exp1Test=
		"	Exp   = Sub / int   \n"+
		"	Sub   = Exp '-' int   \n"+
		"	int   : %digit+   \n";
		
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
		am=ambig.parse("1+2*(3-4/2+1)~9");
		System.out.println(am.text());
		System.out.println(am);
		
	}
	
}
