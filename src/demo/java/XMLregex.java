
import org.spinachtree.gist.*;

import java.util.*;

public class XMLregex {
	
	public static void main(String[] args) {
			
		Gist xml = new Gist(
		"doc     = (xml / char)+ ",
		"xml     = (element / entity / pcdata)+ ",
		"element = '<' name ~ att* ~ ('/>' / '>' content '</' name ~ '>') ",
		"content = (xml / pcdata)* ",
		"att     = name ~ '=' ~ quote ",
		"entity  = '&' (name / '#' (int / 'x' hex)) ';' ",
		"quote   : quot (char-quot)* quot / apos (char-apos)* apos ",
		"name    : xmlName ",
		"pcdata  : (char-'<'-'&')+ ",
		"int     : digit+ ",
		"hex     : xdigit+ " );
		
	System.out.println(xml.inspect());
	
		String filename="target/doc/index.html";
	
		Term t=xml.parse(" ... <p><q>Hi ho</q> <i>new &rarr; world</i> greetings... </p> ...");
		
		System.out.println(t);
	}
}
