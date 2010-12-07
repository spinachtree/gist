
package org.spinachtree.gist;

/**
A Parse object contains the syntax tree or parse fault results.
*/
public class Parse {

	Parse(String input, Span span, int max, Parser parser) {
		this.input=input;
		this.span=span;
		this.max=max;
		this.parser=parser;	
	}

	String input;
	Span span;
	int max;
	Parser parser;
	
	/**
	true for full length parse.
	*/
	public boolean isOK() {return (span.tag>=0 && span.eot==input.length());}
	/**
	true for a parse that matched less than the full input
	*/
	public boolean isPartial() {return (span.tag>=0);}
	// public boolean isFault() {return (span.tag<0);}
	/**
	true if the parse failed
	*/
	public boolean hasFault() {return (span.tag<0);}
	
	// /**
	// Create a Term tree version of the Span tree.
	// */
	// public Term term() { return term(span,parser,input); }
	/**
	Term tree version of the Span syntax tree.
	*/
	public Term termTree() { return term(span,parser,input); }
	/**
	Span syntax tree.
	*/
	public Span spanTree() { return span; }
	/**
	Count Span tree nodes.
	*/
	public int count() { return span.count(); }

	// -- Span access ------------------------------------------------------------------
	
	public int tagId(String name) { return parser.tag(name); }
	public String tagName(Span span) { return parser.tagName(span.tag); }
	public String text(Span span) { return input.substring(span.sot,span.eot); }

	// -- translate Span records into parse tree Terms (tag=>name) --------------------------------------
	Term term(Span span, Parser parser, String src) {
		int tag=span.tag;
		parser=parserLink(tag,parser); // maybe: link, rule
		String name=parser.tagName(tag);
		if (span.tip==span.top) return new Term(name,src.substring(span.sot,span.eot));
		return new Term(name,terms(span.top,span.tip,0,parser,src));
	}

	Term[] terms(Span span, Span floor, int cnt, Parser parser, String src) {
		if (span==floor) return new Term[cnt];
		Term[] kids=terms(span.tip,floor,cnt+1,parser,src);
		kids[kids.length-cnt-1]=term(span,parser,src);
		return kids;
	}
	
	Parser parserLink(int tag, Parser parser) {
		if (tag<0) return parser;
		int link=(tag>>10)&0x3ff;
		if (link==0) return parser;
		return parser.links[link];
	}
	
	// -- Span tree inspection ------------------------------------------------------------------

	public String toString() {
		StringBuilder sb=new StringBuilder();
		if (hasFault()) faultReport("failed",max,sb);
		else if (!isOK()) faultReport("incomplete",span.eot,sb);
		inspect(span,sb);
		return sb.toString();
	}
	
	void faultReport(String msg, int max, StringBuilder sb) {
		sb.append(String.format("Parse %s: %d..%d of %d\n",msg,span.sot,max,input.length()));
		int i=max-35, j=max+35;
		if (i<0) i=0;
		else sb.append(" ... ");
		if (j>input.length()) j=input.length();
		quote(sb,i,max);
		sb.append(" >> ");
		quote(sb,max,j);
		if (j<input.length()) sb.append(" ... ");
		sb.append("\n");
	}

	void inspect(Span span, StringBuilder sb) { inspect(span,"",parser,sb); }
	
	void inspect(Span span, String inset, Parser parser, StringBuilder sb) {
		int tag=span.tag;
		parser=parserLink(tag,parser); // maybe: link, rule
		String name=parser.tagName(tag);
		if (span.tip==span.top) { // leaf terminal
			sb.append(inset).append(name).append(" ");
			quote(sb,span.sot,span.eot);
			sb.append("\n");
		} else {
			sb.append(inset).append(name).append(" ");
			Span[] kids=span.children();
			while (kids!=null && kids.length==1) {
				Span kid=kids[0];
				tag=kid.tag;
				parser=parserLink(tag,parser); // maybe: link, rule
				sb.append(parser.tagName(tag)).append(" ");
				if (kid.tip==kid.top) {
					quote(sb,kid.sot,kid.eot);
					kids=null;
				} else kids=kid.children();
			}
			sb.append("\n");
			if (kids!=null)
				for (Span kid: kids) inspect(kid,inset+"    ",parser,sb);
		}
	}

	void quote(StringBuilder sb, int i, int j) {
		sb.append("\"");
		for (int k=i;k<j;k++) {
			int x=input.charAt(k);
			if (x==92) sb.append("\\");
			else if (x==34) sb.append("\\\"");
			else if (x>31 && x<0XFF) sb.append((char)x);
			else if (x==9) sb.append("\\t");
			else if (x==10) sb.append("\\n");
			else if (x==13) sb.append("\\r");
			else sb.append("\\u"+Integer.toHexString(x));
		}
		sb.append("\"");
	}

} // Parse