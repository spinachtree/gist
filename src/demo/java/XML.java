
import org.spinachtree.gist.*;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class XML {
	
	public static void main(String[] args) 
		throws SAXException, ParserConfigurationException
	{
			
		Gist xml = new Gist(
		"rex = ('<' name / '<' ('/'/'!') / (!'<' %char)+)* ", //"  ((<[A-Za-z0-9]+)|(<[/!])|([^<]+))*",
		"doc     = (xml / char)+ ",
		"xml     = (element / entity / pcdata)+ ",
		"element = '<' name ~ att* ~ ('/>' / '>' content '</' name ~ '>') ",
		"content = (xml / pcdata)* ",
		"att     = name ~ '=' ~ quote ",
		"entity  = '&' (name / '#' (%d+ / 'x' hex+)) ';' ",
		"quote   : %quot (!%quot char)* %quot / %apos (!%apos char)* %apos ",
		"name    : (%alpha/'_'/':') (%alnum/'.'/'-'/'_'/':')* ",
		"pcdata  : (!'<' char)+ ",
		"hex     : '0'..'9'/'A'..'Z'/'a'..'z' ",
		"char    : gist.char " );
		
	System.out.println(xml.inspect());
	
		String filename="target/doc/index.html";
	
		Term t=xml.parse(" ... <p><q>Hi ho</q> <i>new world</i> greetings... </p> ...");
		
		System.out.println(t);
		
		try {
			Term doc=null;

			long gistms0=System.currentTimeMillis();

			Reader reader= new FileReader(filename);
			String src= read(reader);
			//System.out.println(src);
			doc=xml.parse(src);
			long gistms=System.currentTimeMillis()-gistms0;
			
			System.out.println("Gist ms="+gistms);
			
			//----
						
			long rep=1000;
			long start=System.currentTimeMillis();
			for (int i=0;i<rep;i++) {
				doc=xml.parse(src);
			}
			long tms=System.currentTimeMillis()-start;

			//System.out.println(doc);
			long cc=src.length();
			long ec=count(doc,0);
			long ns=tms*1000000/rep;
			System.out.println("rep="+rep+" tot ms="+tms+" ns/parse="+ns+
			" chars="+cc+" char.ns="+ns/cc+" char/ms="+cc*1000000/ns+
			" elems="+ec+" elem.ns="+ns/ec+" elem/ms="+ec*1000000/ns);
		
		// ----------
			
			Pattern pat=Pattern.compile("((<[A-Za-z0-9]+)|(<[/!])|([^<]+))*");
			Matcher m = pat.matcher(src);  //(" ... <p><q>Hi ho</q> <i>new world</i> greetings... </p> ...");
			
			System.out.println("match="+m.matches());
			long rep1=1000;
			long start1=System.currentTimeMillis();
			for (int i=0;i<rep1;i++) {
				m = pat.matcher(src);
				m.matches();
			}
			long tms1=System.currentTimeMillis()-start1;
			int mc=0;
			while(m.find()) mc++;
			System.out.println("rep1="+rep+" tot ms="+tms1+" mc="+mc);

		// ----------

			SAXParserFactory pf=SAXParserFactory.newInstance();
			pf.setValidating(false);
			SAXParser parser=pf.newSAXParser();

			CountHand count=new CountHand();

			long xmlms0=System.currentTimeMillis();
			parser.parse(new File(filename),count);
			long xmlms=System.currentTimeMillis()-xmlms0;
			
			System.out.println("SAX ms="+xmlms+" elems="+count.sum);
			

		} catch (IOException e) { 
			System.out.println(e);
			System.exit(-1);
		}
	}

	static int count(Term t,int n) {
		if (t==null) return 0;
		return 1+count(t.child(),n)+count(t.next(),n);
	}
	
	public static class CountHand extends DefaultHandler {
		int sum=0;
		public void startElement(String uri, String local, String qname, Attributes atts) {
			sum++;
		}
	}
	
	static String read(Reader reader ) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    try {
	        int len = 0;
	        char[] buffer = new char[1024];
	        while ((len=reader.read(buffer)) > -1) sb.append(buffer, 0, len);
	    } finally { 
			reader.close();
    	}
	    return sb.toString();
	}

}
