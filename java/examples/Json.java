
import org.spinachtree.gist.*;

import java.util.*;
import java.math.*;

// JSON -- see: http://www.json.org/

// A very simple translation:

// Json		Java			Example
// -----------|-----------------------|---------------------------------------------
// object	HashMap<Object>		{"foo" : 42, "bar" : ["black","silver"]}
// array	ArrayList<Object>	[42, "gold", "lead"]
// string	String			"this\nand that.."
// number	Number			123.456e-2 => Integer, Long, Float, Double; as necessary

// for a more elaborate implementation of Json in Java see: www.json.org/java/

public class Json {

	String jsonGrammar =
	"	json   =  `s (object | array) `s                 \n"+
	"	object = '{' `s pairs? `s '}'                    \n"+
	"	pairs  =  pair (`s ',' `s pair)*                 \n"+
	"	pair   = string `s ':' `s val                    \n"+
	"	array  = '[' `s vals? `s ']'                     \n"+
	"	vals   =  val (`s ',' `s val)*                   \n"+
	"	val    = object|array|string|number              \n"+
	"	        |true_|false_|null_                      \n"+
	"	string = `quot str `quot                         \n"+
	"	number = int frac?                               \n"+
	"	int    : neg? digits                             \n"+
	"	frac   : '.' digit+ exp?                         \n"+
	"	exp    : ('e'|'E') sign? digit+                  \n"+
	"	digits : '0' | '1'..'9' digit*                   \n"+
	"	digit  : '0'..'9'                                \n"+
	"	sign   : '+'|'-'                                 \n"+
	"	neg    :  '-'                                    \n"+
	"	true_  : 'true'                                  \n"+
	"	false_ : 'false'                                 \n"+
	"	null_  : 'null'                                  \n"+
	"	hex    : digit|'a'..'f'|'A'..'F'                 \n"+
	"	str    : (char|esc)*                              \n"+
	"	esc    : bs (code | 'u' hex hex hex hex)         \n"+
	"	code   : bs|fs|quot|'b'|'f'|'n'|'r'|'t'          \n"+
	"	bs     : 92   -- back-slash                      \n"+
	"	fs     : 47   -- forward-slash                   \n"+
	"	quot   : 34   -- quote                           \n"+
	"	s      : (9|10|13|32)*                           \n"+
	"	char   : 0x20..0x10ffff^quot^bs                  \n";
	
	private Gist json;

	public Json() {
		json=new Gist(jsonGrammar,this);
	}

	public Object read(String text) {
		return json.transform(text);
	}

	// --- transform method for jsonGrammar -----------------------------

	public Object json(Object[] xs) {
		return xs[0];
	}

	public Map<String,Object> object(Object[] xs) {
		Map<String,Object> map=new HashMap();
		Object[] pairs=(Object[])xs[0];
		for (Object pair:pairs) map.put(((Pair)pair).key,((Pair)pair).val);
		return map;
	}

	public List<Object> array(Object[] xs) {
		List<Object> list=new ArrayList();
		Object[] vals=(Object[])xs[0];
		for (Object val:vals) list.add(val);
		return list;
	}

	public Object[] pairs(Object[] xs) {
		return xs;
	}

	public Pair pair(Object[] xs) {
		return new Pair((String)xs[0],xs[1]);
	}

	public Object[] vals(Object[] xs) {
		return xs;
	}

	public String string(Object[] xs) {
		return (String)xs[0];
	}

	public Number number(Object[] xs) {
		String n=(String)xs[0];
		if (xs.length==1) {
			try { return new Integer(n); }
			catch (NumberFormatException e) { return new Long(n); }
		}
		n+=(String)xs[1];
		try { return new Float(n); }
		catch (NumberFormatException e) { return new Double(n); }
	}

	public Boolean true_(Object[] xs) {
		return true;
	}

	public Boolean false_(Object[] xs) {
		return false;
	}

	public Object null_(Object[] xs) {
		return null;
	}
	

	class Pair {

		Pair(String key,Object val) {
			this.key=key;
			this.val=val;
		}
		String key;
		Object val;

	}

	// -- test ---------------------------------------

	public static void main(String[] args) {
		
		String in=" { \"a\" : true, \"b\" : false,"+
			"\"list\" : [123.45, \"one two\", null] }";
		
		if (args.length>0) in=args[0];
		
		Json json=new Json();
		
		System.out.println( in +" =>\n"+ json.read(in) );
	}

}

