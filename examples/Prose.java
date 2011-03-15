
import org.spinachtree.gist.*;

import java.util.*;

public class Prose {
	
	static final String prose=
	"	prose = (item/any)*					\n"+
	"	item  = text/term/xml/symb				\n"+
	"	term  = sigil word? arg*				\n"+
	"	symb  = sym						\n"+
	"	word  : (alnum|'_'|'-')+				\n"+
	"	text  : any^punct+					\n"+
	"	arg   = '(' par ')' | '[' box ']'			\n"+
	"       par   = (lp par rp / !')' item)*			\n"+
	"       box   = (lit/amp/lt/lb box rb)*				\n"+
	"       lit   : (any^'['^']'^'&'^'<')+				\n"+
	"	sigil : '`'|'~'|'!'|'@'|'#'|'^'|'*'|'+'|'.'|':'|','	\n"+
	"	sym   : punct^'('^')'+					\n"+
	"	xml   : '<' tag s* att* s* '/'? '>' | '</' tag s* '>'	\n"+
	"	      | '&' tag ';' | '&#' digit+ ';' | '&#x' hex+ ';'  \n"+
	"	tag   : (alpha|'_'|':') (alnum|'.'|'-'|'_'|':')*	\n"+
	"	att   : tag s* '=' s* quote s*				\n"+
	"	quote : quot (any^quot)* quot | apos (any^apos)* apos	\n"+
	"       quot  : 34					        \n"+
	"       apos  : 39					        \n"+
	"       amp   : '&'					        \n"+
	"       lt    : '<'					        \n"+
	"       lp    : '('					        \n"+
	"       rp    : ')'					        \n"+
	"       lb    : '['					        \n"+
	"       rb    : ']'					        \n"+
	"	digit : '0'..'9'					\n"+
	"	hex   : digit | 'a'..'f' | 'A'..'F'			\n"+
	"	alnum : '0'..'9'|alpha					\n"+
	"	alpha : 'A'..'Z'|'a'..'z'				\n"+
	"	punct : 33..47|58..64|91..96|123..126			\n"+
	"	graph : 33..1114111					\n"+
	"	s     : 9|10|13|32					\n"+
	"	any   : 1..1114111					\n";
	
	
	Gist proseGist;
	
	Prose() { 
		proseGist=new Gist(prose,this);
		//System.out.println(proseGist.ruleCode());
		for (int i=0; i<sigilNames.length; i+=2) sigilTags.put(sigilNames[i],sigilNames[i+1]);
		for (int i=0; i<symbolStrings.length; i+=2) symbols.put(symbolStrings[i],symbolStrings[i+1]);
	}
	
	String transform(String text) { 
		return (String)proseGist.transform(text);
	}

	boolean quotes;
	
	// semantic transform rules..........
	
	public String prose(Object[] xs) {
		StringBuilder sb=new StringBuilder();
		quotes=false;
		for (Object x:xs) sb.append(x.toString());
		return sb.toString();
	}

	public Html term(Object[] xs) { // sigil word? arg*
		String sigil=(String)xs[0];
		if (xs.length==1) return new Html(sigil);
		String tag=sigilTags.get(sigil);
		String cls=null,id=null;
		String val=(String)xs[xs.length-1]; // *val | *cls(val) | *cls[id](val)
		if (xs.length>2) cls=(String)xs[1]; // *cls(val) | *cls[id](val)
		if (xs.length>3) id=(String)xs[2]; // *cls[id](val)
		if (tag.equals("a") && cls==null) cls=val; // cls => href
		if (sigil.equals("#")) cls="#"+cls; // local link
		return new Html(tag,cls,id,val);
	}

	public Html symb(Object[] xs) { // sym 
		return new Html((String)xs[0]);
	}

	public String amp(Object[] xs) { // &
		return "&amp;";
	}

	public String lt(Object[] xs) { // <
		return "&lt;";
	}


	// -------------------------------------------------------------------------
	
	String[] sigilNames = new String[] { 
		"`","tt",    "~","i",     "!","b",   "@","a",   "#","a",
		"%","var",   "^","sup",   "*","em",  "+","strong",
		".","span",   ",","sub",  ":","code"   };

	Map<String,String> sigilTags = new HashMap<String,String>();

	String[] symbolStrings = new String[] { 
		"<","&lt;",     "&","&amp;",   "'","&rsquo;",
		"->","&rarr;",  "<-","&larr;", "<->","&harr;",
		"=>","&rArr;",   "<=","&lArr;", "<=>","&hArr;"  };

	Map<String,String> symbols = new HashMap<String,String>();

	class Html {

		Html(String val) {
			this.tag=null;
			this.cls=null;
			this.id=null;
			this.val=val;
		}

		Html(String tag, String cls, String id, Object val) {
			this.tag=tag;
			this.cls=cls;
			this.id=id;
			this.val=val;
		}
		
		String tag,cls,id;
		Object val;
		
		public String toString() {
			if (tag==null) {
				if (!(val instanceof String)) return val.toString();
				String sym=(String)val;
				if (sym.equals("\"")) {
					quotes=!quotes;
					if (quotes) return "&ldquo;";
					else return "&rdquo;";
				}
				String symb=symbols.get(sym);
				if (symb!=null) return symb;
				sym=sym.replace("&","&amp;");
				sym=sym.replace("<","&lt;");  // in this order :-)
				return sym;
			}
			if (tag.equals("a"))
				if (id==null) return String.format("<a href='%s'>%s</a>",cls,val);
				else return String.format("<a href='%s' id='%s'>%s</a>",cls,id,val);
			if (cls==null) return String.format("<%s>%s</%s>",tag,val,tag);
			if (id==null) return String.format("<%s class='%s'>%s</%s>",tag,cls,val,tag);
			return String.format("<%s class='%s' id='%s'>%s</%s>",tag,cls,id,val,tag);
		}

	}

} //Prose
	
