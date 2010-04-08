
import org.spinachtree.gist.*;

public class PriorTest {
	
	public static void main(String[] args) {
		
		System.out.println("Gist Prior test ========================================================");
		
		String priorTest=
		"	act  =  '<' name '>' txt '</' @name '>'  \n"+
		"       txt  : char-!'<'*  \n"+
		"	@import gist.pragma \n";

		Gist tst=new Gist(priorTest);
		System.out.println(tst.inspect());
		Term t=tst.parse("<foo> hi ho ... </foo>");
		System.out.println(t);

	}
		
	
}
