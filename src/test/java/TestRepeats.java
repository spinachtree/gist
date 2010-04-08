
import org.spinachtree.gist.*;

public class TestRepeats {
	
	public static void main(String[] args) {
		
		String repTest= 
		"	reps  = alpha digit* alpha+ digit^3 alpha^2.._ digit^3..5 0..100*  \n"+
		"	alpha : 'a'..'z'/'A'..'Z'         \n"+
		"	digit : '0'..'9'                  \n";

		Gist reps=new Gist(repTest);
		System.out.println(repTest);
		System.out.println(reps.inspect());
		
		Term t=reps.parse("a123bcd123efgh123456789");
		System.out.println(t);
		
	}
		
	
}
