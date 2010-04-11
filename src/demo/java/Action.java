
import org.spinachtree.gist.*;

public class Action {
	
	public static void main(String[] args) {
		
		String ymdRules =
		
		"Date  = year '-' month  <mnth> '-' day         \n"+
		"year  : d d d d                        \n"+
		"month : d d?                           \n"+
		"day   : d d?                           \n"+
		"d     : '0'..'9'                       \n";

		Gist ymd = new Gist(ymdRules);
		
		Actor act=new Actor();
		ymd.events(act);
		
		Term date=ymd.parse("2009-8-7");
	
		System.out.println("Month = "+date.text("month"));

		System.out.println(date);
	}

}

class Actor implements org.spinachtree.gist.Action {
	
	public boolean event(Scan scan,String rule,String event,String args) {
		System.out.println("Event --> "+event);
		return true;
	}
	
}


