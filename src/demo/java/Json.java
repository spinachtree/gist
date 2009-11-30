
import org.spinachtree.gist.*;

import java.util.*;
import java.math.*;

// JSON -- see: http://www.json.org/

public class Json {
	
	public static void main(String[] args) {
			
		// Gist json = Gist.rules(
		// "JSON    = Object / Array",
		// "Object  = s '{' s Pair (s ',' s Pair)* s '}' s",
		// "Pair    = String s ':' s Value",
		// "Array   = s '[' s Value (s ',' s Value)* s ']' s",
		// "Value   = String / Number / Object / Array / Literal",
		// "String  = quot (bs escape / chrs)* quot",
		// "Number  = neg? int frac? expo?",
		// "Literal : 'true' / 'false' / 'null'",
		// "neg     : '-'",
		// "int     : '0' / '1'..'9' '0'..'9'*",
		// "frac    : '.' '0'..'9'*",
		// "expo    : ('e'/'E') ('+'/'-')? '0'..'9'+",
		// "chrs    : (char-quot-bs)*",
		// "escape  : quot/bs/'/'/'b'/'f'/'n'/'r'/'t'/'u' hex hex hex hex",
		// "hex     : '0'..'9'/'a'..'f'/'A'..'F'",
		// "quot    : 0x22 -- double-quote",
		// "bs      : 0x5C -- backslash",
		// "s       : (9..13/' ')* -- white space",
		// "char    : 0x20..10FFFF -- Unicodes, except control chars");
		
		Gist json = new Gist(
		"	json   = ~ (object / array) ~          ",
		"	object = '{' ~ pairs? ~ '}'            ",
		"	pairs  =  pair (~ ',' ~ pair)*         ",
		"	pair   = string ~ ':' ~ val            ",
		"	array  = '[' ~ vals? ~ ']'             ",
		"	vals   =  val (~','~ val)*             ",
		"	val    = object/array/string/number    ",
		"	         /true/false/null              ",
		"	string = quot.. (chs/esc)* quot..      ",
		"	esc    = bs.. (code / 'u' hex)         ",
		"	number = neg? digits frac? exp?        ",
		"	frac   = '.' int                       ",
		"	exp    = ('e'/'E') sign? int           ",
		"	digits : '0'/'1'..'9' digit*           ",
		"	int    : digit+                        ",
		"	digit  : '0'..'9'                      ",
		"	sign   : '+'/'-'                       ",
		"	neg    :  '-'                          ",
		"	true   : 'true'                        ",
		"	false  : 'false'                       ",
		"	null   : 'null'                        ",
		"	hex    : (digit/'a'..'f'/'A'..'F')*4   ",
		"	code   : bs/fs/quot                    ",
		"	         /'b'/'f'/'n'/'r'/'t'          ",
		"	bs     : 92   -- back-slash            ",
		"	fs     : 47   -- forward-slash         ",
		"	quot   : 34   -- quote                 ",
		"	chs    : char+                         ",
		"	char   : 0x20..10ffff - quot - bs      ");
		
	//	System.out.println(json.inspect());

		String in="{ \"a\" : true, \"b\" : false,"+
			"\"list\" : [123.45, \"one\\ttwo\", null] }";
		if (args.length>0) in=args[0];
		
		Term tree=json.parse(in);
		
		System.out.println( in +" =>\n"+ tree );
	
		if (tree.isTag("json")) { // valid parse tree....
			Object obj=translate(tree.child());
			System.out.println("Java: "+obj);
		}
		
		// List of numbers...
		Term nums=json.parse("[123, -345, 12345678901234567890, -123.45]");
		if (nums.tag()=="json") { // valid parse tree....
			Object arr=translate(nums);
			if (arr instanceof List) {
				List<?> lst=(List<?>)arr;
				for (Object num: lst)
				  System.out.println(num.getClass().getName()+" = "+num);
			}
		} else System.out.println(nums); // fault report
		
		String image=	
		//"{  \"Image\": { \n"+
		"{         \"Width\":  800, \n"+
		"          \"Height\": 600, \n"+
		"          \"Title\":  \"View from:\\t15th Floor\", \n"+
		"          \"Thumbnail\": { \n"+
		"              \"Url\":    \"http://www.example.com/image/481989943\", \n"+
		"              \"Height\": 125, \n"+
		"              \"Width\":  \"100\" \n"+
		"          }, \n"+
		"          \"IDs\": [116, 943, 234, 38793] \n"+
		//"     } \n"+
		" } \n";
		Term it=json.parse(image);
		System.out.println(image +" =>\n"+ it );
		
	}
	
	// Simple translation into Java Objects...
	// JSON allows lists with mixed types, so its hard to do compile-time type checking
	
	static Object translate(Term tot) {
		String tag=tot.tag();
		if (tag=="object") return objectJson(tot);
		else if (tag=="array") return arrayJson(tot);
		else if (tag=="string") return stringJson(tot);
		else if (tag=="number") return numberJson(tot);
		else if (tag=="true") return true;
		else if (tag=="false") return false;
		else if (tag=="null") return null;
		else if (tag=="json") return translate(tot.child());
		else throw new UnsupportedOperationException("json tag: "+tag);
	}
	
	static Map<String,Object>  objectJson(Term obj) {
		// object = '{' s pairs? s '}' 
		// pairs  = pair (s ',' s pair)*
		// pair   = string s ':' s val
		Map<String,Object> result=new HashMap<String,Object>();
		Term pairs=obj.child("pairs");
		if (pairs==null) return result;
		for (Term pair: pairs) {
			String key=pair.text("string");
			Object val=translate(pair.child("val").child());
			result.put(key,val);
		}
		return result;
	}
	
	static List<Object> arrayJson(Term arr) {
		// array  = '[' s vals? s ']'
		// vals   = val (s ',' s val)*
		// val    = object/array/string/number
		//          /true/false/null
		List<Object> result=new ArrayList<Object>();
		Term vals=arr.child("vals");
		if (vals==null) return result;
		for (Term val: vals) {
			Object x=translate(val.child());
			result.add(x);
		}
		return result;
	}
	
	static String stringJson(Term str) {
		// string = quot (chs/esc)* quot
		// esc    = ~bs (code / 'u' hex)
		StringBuilder s=new StringBuilder();
		Term x=str.child();
		while (x!=null) {
			String tag=x.tag();
			if (tag=="chs") s.append(x.text());
			else if (tag=="esc") s.append(escape(x));
			x=x.next();
		}
		return s.toString();
	}
	
	static String escape(Term esc) {
		// esc    = ~bs (code / 'u' hex)
		// code   : bs/fs/quot
		//          /'b'/'f'/'n'/'r'/'t'
		Term code=esc.child("code");
		if (code!=null) {
			String s=code.text();
			if (s.equals("b")) return "\b";
			else if (s.equals("f")) return "\f";
			else if (s.equals("n")) return "\n";
			else if (s.equals("r")) return "\r";
			else if (s.equals("t")) return "\t";
			else return s;
		} else { // hex...
			String hex="0x"+esc.text("hex");
			int ch=Integer.decode(hex);
			return String.valueOf((char)ch);
		}
		
	}
	
	static Number numberJson(Term num) {
		// number = neg? digits frac? exp?
		if (num.child("frac")==null && num.child("exp")==null) {
			try { return new Integer(num.text()); }
			catch (NumberFormatException e) { return new BigInteger(num.text()); }
		}
		try { return new Double(num.text()); }
		catch (NumberFormatException e) { return new BigDecimal(num.text()); }
	}
	
	

}
