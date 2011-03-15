
import org.spinachtree.gist.Gist;
 
public class Test {

	String testGrammar = 
	"	test   = match | fault			\n"+
	"	fault  : 0..1114111*			\n"+
	"       -- match rules added here....		\n";

	// --- transform methods -----------------------------

	public boolean match(Object[] xs) {
		return true;
	}

	public boolean fault(Object[] xs) {
		return false;
	}

	// ---------------------------------------------------

	Gist test;
	int testNum=0;
	
	Test(String match) {
		test=new Gist(testGrammar+match,this);
	}
	
	Test(String match, int testNum) {
		test=new Gist(testGrammar+match,this);
		this.testNum=testNum;
	}
	
	public int runs=0, fails=0;
	
	public void parse(String input) { parse(input,true); }

	public void fail(String input) { parse(input,false); }
	
	public void parse(String input, boolean expect) {
		runs+=1;
		boolean ans=(Boolean)test.transform(input);
		if (ans!=expect) {
			fails+=1;
			if (expect) System.out.printf("Test %d.%d: %s failed to parse... %n",testNum,runs,input);
			else System.out.printf("Test %d.%d: %s was expected to fail... %n",testNum,runs,input);
		}
	}
	
	public String results() {
		return String.format("Test: %d run, %d failed..... %n",runs,fails);
	}
	
	public String ruleCode() {
		return test.ruleCode();
	}
	
	public String toString() {
		return test.toString();
	}

	// --- run a test -----------------------------

	static String matchGrammar = 
	"	match  : alnum^p^q*			\n"+
	"	p      : 'p'				\n"+
	"	q      : 'q'				\n"+
	"	name   : alpha (alnum|'_')*		\n"+
	"	alnum  : alpha|digit			\n"+
	"	alpha  : 'a'..'z'|'A'..'Z'		\n"+
	"	digit  : '0'..'9'			\n"+
	"	s      : (9|10|13|32)*                  \n";
		

	public static void main(String[] args) {
		Test test=new Test(matchGrammar);
		System.out.printf("%s %n",test.ruleCode());
		
		test.parse("abcrstxyz");
		test.fail("abcpqrxyz");
		
		System.out.println(test.results());
	}
	
}


