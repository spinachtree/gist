
import org.spinachtree.gist.*;

public class Date  {

	public static void main(String[] args) {

		// simple and practical...
		
		String ymdRules =

		"Date  = year '-' month '-' day         \n"+
		"year  : d d d d                        \n"+
		"month : d d?                           \n"+
		"day   : d d?                           \n"+
		"d     : '0'..'9'                       \n";

	   // better date range: 1000..2999 '-' 1..12 '-' 1..31  ...
	
		String ymdGist =
		
		"Date  = year '-' month '-' day                                          \n"+
		"year  : '1'..'2' d d d                            -- 1000..2999         \n"+
		"month : '0'? '1'..'9' | '1' '0'..'2'              -- 1..12, inc 01..09  \n"+
		"day   : '0'? '1'..'9' | '1'..'2' d | '3' '0'..'1' -- 1..31, inc 01..09  \n"+
		"d     : '0'..'9'                                                        \n";

	   // month days date range: 1000..2999 '-' 1..12 '-' 1..29|30|31  ...

		String ymdSpec =
	
		"Date    = year '-' month_ '-' day                                         \n"+
		"year    : '1'..'2' d d d     -- 1000..2999                                \n"+
		"month_  = feb | sajn | rest  -- 30 days hath sep. apr. june. and nov...   \n"+
		"feb     : '0'? '2'                                                        \n"+
		"sajn    : '0'? ('9'|'4'|'6') | '11'   -- sep. apr. june. and nov.         \n"+
		"rest    : '0'? ('1'|'3'|'5'|'7'..'8') | '10' | '12'                       \n"+
		"day     : '0'? '1'..'9' | '1'..'2' d  -- 1..29                            \n"+
		"	     | @sajn '30'                                                      \n"+
		"	     | @rest '3' '0'..'1'                                              \n"+
		"d       : '0'..'9'                                                        \n";

	   // leap years: even better date range: 1900..2399 - 1..12 '-' 1..28|29|30|31  ...

		String ymdLeap =

		"Date  = year '-' month '-' day                                          \n"+
		"year  = leap / norm                                                     \n"+
		"leap  : ('19'|'2' '0'..'3') !'00' d ('0'|'4'|'8') / '2000'              \n"+
		"norm  : ('19'|'2' '0'..'3') d d  -- 1900..2399                          \n"+
		"month = feb | sajn | rest  -- 30 days hath sep. apr. june. and nov...   \n"+
		"feb   : '0'? '2'                                                        \n"+ 
		"sajn  : '0'? ('9'|'4'|'6') | '11'   -- sep. apr. june. and nov.         \n"+
		"rest  : '0'? ('1'|'3'|'5'|'7'..'8') | '10' | '12'                       \n"+
		"day   : '0'? '1'..'9' | '1' d | '2' '0'..'8'  -- 1..28                  \n"+
		"      | @month.feb @year.leap '29'                                      \n"+
		"      | @month.sajn ('29'/'30')                                         \n"+
		"	   | @month.rest ('29'/'30'/'31')                                    \n"+
		"d     : '0'..'9'                                                        \n";

		//Gist date = new Gist(ymdRules);
		//Gist date = new Gist(ymdSpec);
		Gist date = new Gist(ymdLeap);
		//date.dump();
		Parse dateTree = date.parse("2001-4-30");
		System.out.println(dateTree);
		dateTree = date.parse("2001-4-31");
		System.out.println(dateTree);
		dateTree = date.parse("2005-2-29");
		System.out.println(dateTree);
		dateTree = date.parse("2008-2-29");
		System.out.println(dateTree);
		
	}

}
