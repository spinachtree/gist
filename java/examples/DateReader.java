
import org.spinachtree.gist.Gist;

// Simple first example...

public class DateReader {

	public final String dateGrammar = 
	"	date = year '-' month '-' day		\n"+
	"	year : d d d d				\n"+
	"	month: d d?				\n"+
	"	day  : d d?				\n"+
	"	d    : '0'..'9'				";
		
	private Gist dateGist;
	
	public DateReader() {
		dateGist=new Gist(dateGrammar);
	}
	
	public Object[] ymd(String date) {
		// parse and transform date string input...
		return ((Object[]) dateGist.transform(date));
	}

	// -- test -----------------------------------------
	
	public static void main(String[] args) {
		DateReader dr = new DateReader();
		Object[] ymd = dr.ymd("2001-2-3");
		String y=(String)ymd[0], m=(String)ymd[1], d=(String)ymd[2];
		System.out.printf("year=%s month=%s day=%s %n",y,m,d);
	}

} // Date


