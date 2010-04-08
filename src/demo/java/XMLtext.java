
import org.spinachtree.gist.*;

public class XMLtext {

	static final Gist xmlChars = new Gist(
	
		"text = (lt/amp/txt)*  ",
		"lt   : '<'            ",
		"amp  : '&'            ",
		"txt  : (char-!lt-!amp)* ",
		"char : 0x1..10ffff    " );

	public static void main(String[] args) {
                String in="if (X < Y && Z) then ...";
                if (args.length>0) in=args[0];
                System.out.println( in +" => "+ xmlText(in) );
	}	

	static String xmlText(String in) {
		Term tree=xmlChars.parse(in);
		StringBuilder out=new StringBuilder();
		for (Term term: tree) {
		    if (term.isTag("lt")) out.append("&lt;");
		    else if (term.isTag("amp")) out.append("&amp;");
		    else out.append(term.text());
		}
		return out.toString();
	}
}

