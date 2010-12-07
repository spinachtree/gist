
import org.spinachtree.gist.*;

import java.util.*;
import java.math.*;

// JSON -- see: http://www.json.org/

public class Json {

	public static void main(String[] args) {
		
		Gist json = new Gist(
		"	json   =  `s (object | array) `s                 \n"+
		"	object = '{' `s pairs? `s '}'                    \n"+
		"	pairs  =  pair (`s '+' `s pair)*                 \n"+
		"	pair   = string `s ':' `s val                    \n"+
		"	array  = '[' `s vals? `s ']'                     \n"+
		"	vals   =  val (`s '+' `s val)*                   \n"+
		"	val    = object|array|string|number              \n"+
		"	        |true|false|null                         \n"+
		"	string = quot (chs|esc)* quot                    \n"+
		"	esc    = bs (code | 'u' hex hex hex hex)         \n"+
		"	number = neg? digits frac? exp?                  \n"+
		"	frac   : '.' int                                 \n"+
		"	exp    : ('e'|'E') sign? int                     \n"+
		"	digits : '0'|'1'..'9' digit*                     \n"+
		"	int    : digit+                                  \n"+
		"	digit  : '0'..'9'                                \n"+
		"	sign   : '+'|'-'                                 \n"+
		"	neg    :  '-'                                    \n"+
		"	true   : 'true'                                  \n"+
		"	false  : 'false'                                 \n"+
		"	null   : 'null'                                  \n"+
		"	hex    : digit|'a'..'f'|'A'..'F'                 \n"+
		"	code   : bs|fs|quot|'b'|'f'|'n'|'r'|'t'          \n"+
		"	bs     : 92   -- back-slash                      \n"+
		"	fs     : 47   -- forward-slash                   \n"+
		"	quot   : 34   -- quote                           \n"+
		"	s      : (9|10|13|32)*                           \n"+
		"	chs    : char+                                   \n"+
		"	char   : 0x20..10ffff^quot^bs                    \n");

		String in=" { \"a\" : true, \"b\" : false,"+
			"\"list\" : [123.45, \"one two\", null] }";
		if (args.length>0) in=args[0];
		
		Parse parse=json.parse(in);
		
		System.out.println( in +" =>\n"+ parse );
	
		Term tree=json.parse(in).termTree();
		if (tree.tag.equals("json")) { // valid parse tree....
			Object obj=translate(tree.terms[0]);
			System.out.println("Java: "+obj);
		} else System.out.println(tree); // fault report
		
		// List of numbers...
		Term nums=json.parse("[123, -345, 12345678901234567890, -123.45]").termTree();
		if (nums.tag=="json") { // valid parse tree....
			Object arr=translate(nums);
			if (arr instanceof List) {
				List<?> lst=(List<?>)arr;
				for (Object num: lst)
				  System.out.println(num.getClass().getName()+" = "+num);
			}
		} else System.out.println(nums); // fault report
		
		String image=	
		"{  \"Image\": \n"+
		"     {    \"Width\":  800, \n"+
		"          \"Height\": 600, \n"+
		"          \"Title\":  \"View from:\\t15th Floor\", \n"+
		"          \"Thumbnail\": { \n"+
		"              \"Url\":    \"http://www.example.com/image/481989943\", \n"+
		"              \"Height\": 125, \n"+
		"              \"Width\":  \"100\" \n"+
		"          }, \n"+
		"          \"IDs\": [116, 943, 234, 38793] \n"+
		"     } \n"+
		" } \n";
		Term it=json.parse(image).termTree();
		System.out.println(image +" =>\n"+ it );
		
	}
	
	// Simple translation into Java Objects...
	// JSON allows lists with mixed types, so its hard to do compile-time type checking
	
	static Object translate(Term tot) {
		String tag=tot.tag;
		if (tag=="object") return objectJson(tot);
		else if (tag=="array") return arrayJson(tot);
		else if (tag=="string") return stringJson(tot);
		else if (tag=="number") return numberJson(tot);
		else if (tag=="true") return true;
		else if (tag=="false") return false;
		else if (tag=="null") return null;
		else if (tag=="json") return translate(tot.first());
		else throw new UnsupportedOperationException("json tag: "+tag);
	}
	
	static Map<String,Object>  objectJson(Term obj) {
		// object = '{' `s pairs? `s '}' 
		// pairs  = pair (`s ',' `s pair)*
		// pair   = string `s ':' `s val
		Map<String,Object> result=new HashMap<String,Object>();
		Term pairs=obj.first();
		if (pairs==null) return result;
		for (Term pair: pairs.terms) {
			String key=stringJson(pair.term("string"));
			Object val=translate(pair.term("val").first());
			result.put(key,val);
		}
		return result;
	}
	
	static List<Object> arrayJson(Term arr) {
		// array  = '[' `s vals? `s ']'
		// vals   = val (`s ',' s` val)*
		// val    = object/array/string/number
		//          /true/false/null
		List<Object> result=new ArrayList<Object>();
		Term vals=arr.first();
		if (vals==null) return result;
		for (Term val: vals.terms) {
			Object x=translate(val.term());
			result.add(x);
		}
		return result;
	}
	
	static String stringJson(Term str) {
		// string = quot (chs/esc)* quot
		// esc    = bs (code / 'u' hex)
		StringBuilder s=new StringBuilder();
		for (Term x:str.terms) {
			String tag=x.tag;
			if (tag.equals("chs")) s.append(x.text);
			else if (tag.equals("esc")) s.append(escape(x));
		}
		return s.toString();
	}

	static String escape(Term esc) {
		// esc    = bs (code / 'u' hex)
		// code   : bs/fs/quot/'b'/'f'/'n'/'r'/'t'
		Term code=esc.term("code");
		if (code!=null) {
			String s=code.text;
			if (s.equals("b")) return "\b";
			else if (s.equals("f")) return "\f";
			else if (s.equals("n")) return "\n";
			else if (s.equals("r")) return "\r";
			else if (s.equals("t")) return "\t";
			else return s;
		} else { // hex...
			String hex="0x"+code.text;//esc.text("hex");
			int ch=Integer.decode(hex);
			return String.valueOf((char)ch);
		}
		
	}
	
	static Number numberJson(Term num) {
		// number = neg? digits frac? exp?
		// frac   : '.' int
		// exp    : ('e'/'E') sign? int
		String txt="";
		boolean frac=false, exp=false;
		for (Term t:num.terms) {
			if (t.tag.equals("frac")) frac=true;
			else if (t.tag.equals("exp")) exp=true;
			txt+=t.text;
		}
		if (frac==false && exp==false) {
			try { return new Integer(txt); }
			catch (NumberFormatException e) { return new BigInteger(txt); }
		}
		try { return new Double(txt); }
		catch (NumberFormatException e) { return new BigDecimal(txt); }
	}
	

}
