
import org.spinachtree.gist.*;

public class Date1 {

	static final Gist ymd = new Gist(
	
	"Date  = year '-' month '-' day  ",
	"year  : d d d d                 ",
	"month : d d?                    ",
	"day   : d d?                    ",
	"d     : '0'..'9'                ");

	public static void main(String[] args) {
	
		String in="2009-8-7";
		if (args.length>0) in=args[0];
		
		Term date=ymd.parse(in);

		if (date.isTag("Date"))
			System.out.println("Month = "+date.text("month"));
		else
			System.out.println(date);
	}
}
