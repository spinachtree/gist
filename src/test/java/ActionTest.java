
import org.spinachtree.gist.*;

public class ActionTest {
	
	public static void main(String[] args) {
		
		System.out.println("Gist Action test ========================================================");
		
		String actionTest=
		"	act  =  alpha <tr hiho..> alnum* \n"+
		"	alpha : 'a'..'z'/'A'..'Z'        \n"+
		"	alnum : alpha/'0'..'9'        \n";

		Gist tst=new Gist(actionTest);
		System.out.println(tst.inspect());
		Term t=tst.parse("x42");
		System.out.println(t);

	}
		
	
}
