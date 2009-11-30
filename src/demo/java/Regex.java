import org.spinachtree.gist.*;

import java.util.*;

public class Regex {
	
	public static void main(String[] args) {
	
                String pat=" '%' alpha+ ";
                String text="Some %this and %that or %other stuff.";
                if (args.length>0) pat=args[0];
                if (args.length>1) text=args[1];
		
		List<String> matched=findAll(pat,text);
                for (String match: matched) System.out.println(match);
	}	

	static List<String> findAll(String pat,String text) {
		List<String> list=new ArrayList<String>();
        //        String rls="match = (find/char..)* \n find : '%' alpha \n alpha : 'a..'z'/'A'..'Z' \n";
	//	Gist matcher=new Gist(rls);
		Gist matcher = new Gist(
		"match = (find/char..)* ",
		"find  : "+pat,  // eg "'%' alpha+ ",
		"alpha : 'a'..'z'/'A'..'Z' ",
		"char  : _ ");
                Term match=matcher.parse(text);
                if (match.isTag("match"))
                        for (Term found: match) list.add(found.text());
                else
                        System.out.println(match); // fault report
                return list;
        }
}
