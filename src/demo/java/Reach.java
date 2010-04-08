
import org.spinachtree.gist.*;

public class Reach {
	
	public static void main(String[] args) {
		
		String reachRules =
		
		"s = x* / y         \n"+
		"x : int                        \n"+
		"y : int                          \n"+
		"int : '0'..'9'+                      \n";

		Gist reach = new Gist(reachRules);
	System.out.println(reach);	
		Term tst=reach.parse("a123");
	
		System.out.println(tst);
	}

}

