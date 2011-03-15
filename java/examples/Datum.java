
import org.spinachtree.gist.Gist;

import java.util.Date;
import java.util.Calendar;

// Simple first example...

public class Datum {

	public final String dateGrammar = 
	"	date = year '-' month '-' day		\n"+
	"	year : d d d d				\n"+
	"	month: d d?				\n"+
	"	day  : d d?				\n"+
	"	d    : '0'..'9'				";
		
	private Gist dateGist;
	
	public Datum() {
		// create Gist transformer with grammar and rule listener..
		dateGist=new Gist(dateGrammar,this);
	}
	
	public Date of(String date) {
		// parse and transform date string input...
		return (Date)dateGist.transform(date);
	}

	// --- transform method for dateGrammar -----------------------------
	
	public Date date(Object[] ymd) {
		String y=(String)ymd[0], m=(String)ymd[1], d=(String)ymd[2];
		System.out.printf("year=%s month=%s day=%s %n",y,m,d);
		Calendar c=Calendar.getInstance();
		c.set(new Integer(y),new Integer(m),new Integer(d));
		return new Date(c.getTimeInMillis());
	}
	
	// -- test -----
	
	public static void main(String[] args) {
		Date date=new Datum().of("2001-2-3");
		System.out.println(date);
	}

} // Datum


