
import org.spinachtree.gist.*;

public class Overruns {
	
	public static void main(String[] args) {
		
		System.out.println("Gist Overruns test ========================================================");
		
		String overrunTest=
		"	Over  = alnum* alpha \n"+
		"	alpha : 'a'..'z'/'A'..'Z'        \n"+
		"	alnum : alpha/'0'..'9'        \n";

		Gist tst=new Gist(overrunTest);
		System.out.println(tst.inspect());
		Term t=tst.parse("x42");
		System.out.println(t);

	}
		
	
}
