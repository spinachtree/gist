import org.spinachtree.gist.*;

import java.util.*;

public class Regex1 {
	

	public static void main(String[] args) {
	
	        String in="Some %this and %that or %other stuff.";
		Gist.load("Finder","find : '%' alpha alnum* ; _ -> gist.pragma");

		List<String> matched=findAll(in);
                for (String match: matched) System.out.println(match);
	}
	
	static List<String> findAll(String in) {
		
		Gist matcher = new Gist(
			"match = (find/char..)* ",
			"char  : _ ",
			"find  -> Finder ;      ");

		List<String> list=new ArrayList<String>();
                Term match=matcher.parse(in);
                if (match.isTag("match"))
                        for (Term found: match) list.add(found.text());
                else
                        System.out.println(match); // fault report
                return list;
        }
}
