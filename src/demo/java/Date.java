
import org.spinachtree.gist.*;

public class Date {
	
	public static void main(String[] args) {
		
		String ymdRules =
		
		"Date  = year '-' month '-' day         \n"+
		"year  : d d d d                        \n"+
		"month : d d?                           \n"+
		"day   : d d?                           \n"+
		"d     : '0'..'9'                       \n";

		Gist ymd = new Gist(ymdRules);
		
		Term date=ymd.parse("2009-8-7");
	
		System.out.println("Month = "+date.text("month"));

		System.out.println(date);
	}

}


// class DateEvent implements Plugin {
// 	public boolean event(String name,String args,Scan scan) {
// 		System.out.println("Hellow World!");
// 		return true;
// 	}
// }

