
import org.spinachtree.gist.*;
import java.io.*;
import java.util.*;

public class Tad {
	
	public static void main(String[] args) {
		if (args.length==1) new Tad(args[0]).html();
		else if (args.length==2) new Tad(args[0],args[1]).html();
		else System.exit(-1);
	}
	
	Tad(String fileName) {
		filein=fileName;
		fileout=filein+".html";
	}
	
	Tad(String file1, String file2) {
		filein=file1;
		fileout=file2;
	}
	
	String filein;
	String fileout;
	StringBuilder out=new StringBuilder();
	
//	void build(String str) { out.append(str); }
	
	Map<String,Term> formats=new HashMap<String,Term>();
	Map<String,Term> defineId=new HashMap<String,Term>();

	void html() {
	    
		Gist tad=new Gist( // top level tag items (visual space).....
        "	doc   = (item / text / eol..)*	 		\n"+
        "	item  = tag data				\n"+
        "	tag   : black*					\n"+
        "	data  = zig.. line (zig.. line / gap line?)*	\n"+
        "	line  = head (neck.. tail)?			\n"+
        "	text  : chr+ (nl !(key zig) chr+)*		\n"+
        "	key   : black*					\n"+
        "	neck  : tab+ fold? / fold				\n"+
        "	head  : field						    \n"+
        "	tail  : field ((zag/fold) field)*		\n"+
        "	field : (chr-tab)+						\n"+
        "	fold  : nl? tab tab+					\n"+
        "	zig   : nl? zag							\n"+
        "	gap   : eol* zag						\n"+
        "	zag   : tab tos* / ' ' tos+				\n"+
        "	tos   : tab / ' '						\n"+
        "	tab   : 9   							\n"+
        "	black : chr - 1..32				    	\n"+
        "	nl    : 13 (10/0x85)? / eol				\n"+
        "	eol   : 10/13/0x85/0x2028 				\n"+
        "	chr   : 0x1..10ffff - eol				\n");

        // Gist tad=new Gist( // top level tag items (visual space).....
        // "    doc   = (item / text / eol..)*          \n"+
        // "    item  = tag data                        \n"+
        // "    tag   = key zig..                       \n"+
        // "    text  = line (nl.. !tag line)*          \n"+
        // "    data  = line (zig.. line / gap line?)*  \n"+
        // "   key   : black*                           \n"+
        // "    line  : chr+                            \n"+
        // "    zig   : nl? zag                         \n"+
        // "    gap   : eol* zag                        \n"+
        // "    zag   : tab tos* / ' ' tos+             \n"+
        // "    tos   : tab / ' '                       \n"+
        // "    tab   : 9                               \n"+
        // "    black : chr - 1..32                     \n"+
        // "    nl    : 13 (10/0x85)? / eol             \n"+
        // "    eol   : 10/13/0x85/0x2028               \n"+
        // "    chr   : 0x1..10ffff - eol               \n");

		//System.out.println(tadDoc);
		//System.out.println(tad.inspect());
		// System.out.println(doc);
        
		String src=readFile(filein);
		Term doc=tad.parse(src);

		//System.out.println(doc);


		System.out.println("-------------------------------------");
		
        // builtins................................
		out.append("<html><head>");
        
        StringBuilder styles=new StringBuilder();
        
    	for (Term item : doc) {
    	    if (item.isTag("item")) {
    	        // item = tag data
                // data = line (zig.. line / gap line?)*
                // line = head (neck.. tail)?
    	        String tag=item.text("tag");
    	        if (tag.equals("style"))
    	            styles.append(item.text("data"));
    	        else if (tag.equals("format")) {
                     for (Term line : item.child("data")) {
                        if (line.isTag("line")) {
                            String head=line.text("head");
                            Term tail=line.child("tail");
                            formats.put(head,tail);
                        }
                    }
                } else if (tag.equals("define"))
                    for (Term line : item.child("data"))
                        if (line.isTag("line")) defineId.put(line.text("head"),line);
     	    }
    	}
    	
       	out.append("<style type='text/css'>");
    	out.append(styles.toString());
       	out.append("</style>");

		// render....................................
    	out.append("</head><body>\n");
		
	//Term tot=doc.child();
	//while (tot!=null) {
	for (Term tot : doc) {
		if (tot.isTag("text")) { 
			out.append("<p>");
			renderProse(tot.text());
			out.append("</p>\n");
		} else if (tot.isTag("item")) {
			// item = tag data
			String tag=tot.text("tag");
			Term format=formats.get(tag);
			if (format!=null)
				renderForm(format,null,tot.text("data"));
			else if (tag.equals("define") ||
					tag.equals("def") ||
					tag.equals("dl")) {
				// item = tag data
				// data = line (zig.. line / gap line?)*
				// line = head (neck.. tail)?
				out.append("<dl class='"+tag+"'>");
				for (Term line : tot.child("data")) {
					if (line.isTag("line")) {
						String head=line.text("head");
						out.append("<dt id='"+head+"'>");
						renderText(head);
						out.append("</dt><dd>");
						renderProse(line.text("tail"));
						out.append("</dd>\n");
					}
				}
				out.append("</dl>\n");
			} else if (tag.equals("*")) {
				Term p=tot.prior();
				Term nxt=tot.next();
				if (p==null || !(p.isTag("item") && p.text("tag").equals("*")))
					out.append("<ul>");
				out.append("<li>");
				renderProse(tot.text("data"));
				out.append("</li>");
				if (nxt==null || !(nxt.isTag("item") && nxt.text("tag").equals("*")))
					out.append("</ul>");
			} else if (!tag.equals("format") && !tag.equals("style")) {
				out.append("<pre>");
				renderText(tot.text());
				out.append("</pre>\n");
			}
		} // ..item
	//	tot=tot.next();
	} // ..doc

	out.append("</body></html>");
	writeFile(fileout,out.toString());
	
	} // .html
	
	static Gist form=new Gist(
	    "form = ('%' label / chrs)* ",
	    "label: ('a'..'z'/'A'..'Z'/'0'..'9')+ ",
	    "chrs : char (char-'%')* ",
	    "char : 0x1..10ffff");

    void renderForm(Term format,String prefix,String data) {
        Term formatTree=form.parse(format.text());
        for (Term fmt: formatTree) {
            if (fmt.isTag("chrs")) out.append(fmt.text());
    	    else if (fmt.isTag("label")) {
                if (fmt.isText("data")) out.append(data);
                else if (fmt.isText("text")) renderText(data);
                else if (fmt.isText("prose")) renderProse(data);
                else if (fmt.isText("prefix") && prefix!=null) renderText(prefix);
                else out.append(fmt.text());
            }
            else out.append(fmt.text()); // undefined form node
        }
    }

	Gist proseForm=new Gist(
	"prose = (quote/par/box/brace/symb/chars)* ",
	"quote = '\"' pre? qod '\"' ",
	"qod   : (char-'\"')* ",
	"par   = '(' pre? pod ')' ",
	"pod   : (par/char-')')* ",
	"box   = '[' pre? bod ']' ",
	"bod   : (box/char-']')* ",
	"brace = '{' pre? bra '}' ",
	"bra   : (brace/char-'}')* ",
	"pre   = name mark ",
	"name  : (alpha alnum*)? ",
	"alpha : 'a'..'z'/'A'..'Z' ",
	"alnum : alpha/'0'..'9' ",
	"symb  = sigil word? ",
	"sigil : punct (punct-red)* ",
	"word  : alnum+ ", // should be Unicode token
	"mark  : punct-red ",
	"red   : '<'/'&'/'\"'/'('/')'/'['/']'/'{'/'}' ",
	"punct : 33..47/58..64/91..96/123..126 ",
	"chars : char (char-punct)* ",
	"char  : 0x1..10ffff " );

	void renderProse(String str) {
		Term proseTree=proseForm.parse(str);
		for (Term node: proseTree) {
		   if (node.isTag("chars")) out.append(node.text());
		   else if (node.isTag("lt")) out.append("&lt;");
		   else if (node.isTag("amp")) out.append("&amp;");
		   else if (node.isTag("symb")) renderSymbol(node);
		   else renderPattern(node); // eg [prefix:data...] etal
		}
 	}
    
	void renderPattern(Term node) {
		// [prefix:body] => x prefix y body z
		//  pre = name? mark
		String src=node.text(); // eg [...]
		int k=src.length(); 
		String x=src.substring(0,1);    // x = '['
		String z=src.substring(k-1,k);  // z = ']'
		Term format=formats.get(src);
		Term pre=node.child("pre"); // eg [pre ... ]
		if (pre==null) { // no pattern, expected common case, eg [foo bar ...]
			if (format==null) format=formats.get(x+"_"+z); // eg [_]
			if (format==null) renderAsProse(src); // most common case
			else renderForm(format,"",src.substring(1,k-1)); // format match
			return; // all done when no pattern in src...
		} // there is a pattern, it may have a match, eg [prefix:data] ...........
		String y=pre.text("mark");      // y = ':'
		String prefix=pre.text("name"); // pre = name mark
		String data=""; // default
		if (pre.next()!=null) data=pre.next().text();
		if (format==null && defineId.get(data)!=null) {
		    format=formats.get(x+prefix+y+"?"+z); // [prefix:?]
		    if (format==null) format=formats.get(x+"_"+y+"?"+z); // [_:?]
		}
		if (format==null) format=formats.get(x+prefix+y+"_"+z); // [xxx:_]
		if (format==null) format=formats.get(x+"_"+y+"_"+z); // [_:_]
		if (format!=null) { renderForm(format,prefix,data); return; }
		// no prefix pattern match, try for a full length match...
		format=formats.get(x+"_"+z); // eg [_]
		if (format!=null) { renderForm(format,"",src.substring(1,k-1)); return; }
//System.out.println("format:"+src.substring(1,k-1)); return; }
		renderAsProse(src); // no format matched
	}

	void renderAsProse(String src) {
//System.out.println("asProse:"+src);
		int k=src.length(); 
		out.append(src.charAt(0));
		renderProse(src.substring(1,k-1)); 
		out.append(src.charAt(k-1));
	}

	//     void renderPattern(Term node) {
	//         // [prefix:body] => x prefix y body z
	//         //  pre = name? mark
	//         String src=node.text(); // eg [...]
	//         Term format=formats.get(src);  // [xxxx] fully specific match
	// if (format!=null) { renderForm(format,"",src); return; }
	//         int k=src.length();
	//         String x=src.substring(0,1);   // x = '['
	//         String z=src.substring(k-1,k); // z = ']'
	//         Term pre=node.child();
	//         if (pre.next()==null) { // no prefix...
	//     format=formats.get(x+"?"+z); // [?]
	//     if (format!=null && defineId.get(src.substring(1,k-1))!=null) {
	// 	renderForm(format,"",src.substring(1,k-1));
	// 	return;
	//     }
	//             format=formats.get(x+"_"+z); // [_]
	//             if (format==null) renderText(src); // no match
	//     else renderForm(format,"",src.substring(1,k-1));
	//         } else { // prefix.....
	//             String prefix=pre.text("name");     // pre = name? mark
	//             String y=pre.text("mark");          // eg y = ':'
	//             String data=pre.next().text();
	//             if (defineId.get(data)!=null) {
	//                 format=formats.get(x+prefix+y+"?"+z); // [prefix:?]
	//                 if (format==null) format=formats.get(x+"_"+y+"?"+z); // [_:?]
	//             }
	//             if (format==null) format=formats.get(x+prefix+y+"_"+z); // [hd:_]
	//             if (format==null) format=formats.get(x+"_"+y+"_"+z); // [_:_]
	//     if (format==null) {
	// 	format=formats.get(x+"_"+z); // [_]
	// 	if (format==null) renderText(src); // nothing matched
	// 	else renderForm(format,prefix,src.substring(1,k-1));
	//     } else renderForm(format,prefix,data);
	//         }
	//     }

	void renderSymbol(Term node) {
		// symb  = sigil word?
		// sigil : punct punct-red*
		// red   : '<'/'&'/'\"'/'('/')'/'['/']'/'{'/'}'
	        String src=node.text(); // eg --->
		String sigil=node.text("sigil");
		String word=node.text("word");
		Term format=formats.get(src); // --->   specific match
		if (format!=null) { renderForm(format,null,src); return; }
		if (word!="") { // eg: ^_ or ^?
			if (defineId.get(word)!=null)
				format=formats.get(sigil+'?');
			if (format==null)
				format=formats.get(sigil+'_');
			if (format!=null) { renderForm(format,sigil,word); return; }
		} 
		renderText(node.text());
	}

	void renderText(String str) {
		Gist text=new Gist(
		    "text= (lt/amp/txt)* ",
		    "lt  : '<' ",
		    "amp : '&' ",
		    "txt : (char-lt-amp)* ",
		    "char: 0x1..10ffff");
		Term tree=text.parse(str);
		for (Term node: tree) {
		    if (node.isTag("lt")) out.append("&lt;");
	    	else if (node.isTag("amp")) out.append("&amp;");
		    else out.append(node.text());
		}
	}

    // file i/o ---------------------------------------------
    
    String readFile(String file) {
    	String src=null;
    	try { Reader reader = new FileReader(file);
    	    src=read(reader);
    	} catch (IOException e) { 
    		System.out.println(e);
    		System.exit(-1);
    	}
		return src;
    }

	String read(Reader reader ) throws IOException {
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

    void writeFile(String file,String dst) {
    	Writer writer = null;
    	try { writer = new FileWriter(file);
    	    writer.write(dst);
    		writer.close();
    	} catch (IOException e) { 
    		System.out.println(e);
    		System.exit(-1);
        }
    }

}
