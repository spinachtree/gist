
import org.spinachtree.gist.Gist;
import java.util.*;

// Simple first example...

public class DateMap {

	public final String dateGrammar = 
	"	date = { year '-' month '-' day }	\n"+
	"	year : d d d d				\n"+
	"	month: d d?				\n"+
	"	day  : d d?				\n"+
	"	d    : '0'..'9'				";
		
	private Gist dateGist;
	
	public DateMap() {
		dateGist=new Gist(dateGrammar);
	}
	
	public Map<String,Object> ymd(String date) {
		// parse and transform date string input...
		return ((Map<String,Object>) dateGist.transform(date));
	}

	// -- test -----------------------------------------
	
	public static void main(String[] args) {
		DateMap dm = new DateMap();
		Map<String,Object> ymd = dm.ymd("2001-2-3");
		System.out.println("year  = "+(String)ymd.get("year"));
		System.out.println("month = "+(String)ymd.get("month"));
		System.out.println("day   = "+(String)ymd.get("day"));
	}

} // Date


