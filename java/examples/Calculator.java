
import org.spinachtree.gist.Gist;
 
// Usage:
//	Calculator myCalc=new Calculator();
//	int result1=myCalc.eval("1+2+3*(3-4/2+1)-3");
//	int result2=myCalc.eval("1+2*3");
//		or
//	int result=new Calculator().eval("1+2+3*(3-4/2+1)-3");
//

public class Calculator {

	public final String calcGrammar = 
	"	arith = term (add term)*		\n"+
	"	term  = val (mult val)*			\n"+
	"	val   = num | '(' arith ')'		\n"+
	"	add   : '+' | '-'			\n"+
	"	mult  : '*' | '/'			\n"+
	"	num   : '0'..'9'+			";
		
	private Gist calc;
	
	public Calculator() {
		// create Gist transformer with grammar and rule listener..
		calc=new Gist(calcGrammar,this);
	}
	
	public int eval(String expression) {
		return ((Integer)calc.transform(expression));
	}

	// --- transform method for calcGrammar -----------------------------
	
	public Integer arith(Object[] args) {
		if (args.length==1) return (Integer)args[0];
		return reduce((Integer)args[0],1,args);
	}
	
	public Integer term(Object[] args) {
		return arith(args);
	}
	
	public Integer val(Object[] args) {
		Object x=args[0];
		if (x instanceof String) return new Integer((String)x);
		return (Integer)x;
	}

	// --- end transform method for calcGrammar -----------------------------

	Integer reduce(int x, int i, Object[] args) {
		if (i>=args.length) return x;
		String op = (String)args[i];
		int y = (Integer)args[i+1];
		return reduce(calc(x,op,y),i+2,args);
	}
		
	int calc(int x, String op, int y) {
		if (op.equals("+")) return (x+y);
		if (op.equals("-")) return (x-y);
		if (op.equals("*")) return (x*y);
		if (op.equals("/")) return (x/y);
		return 0; // should throw exception
	}
		
	public static void main(String[] args) {
		String input="2+5*8";
		if (args.length>0) input=args[0];
		Integer ans=new Calculator().eval(input);
		System.out.printf("%s => %s %n",input,ans.intValue());
	}

} // Calcualator


