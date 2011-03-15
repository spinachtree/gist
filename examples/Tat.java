
import org.spinachtree.gist.*;

import java.net.*;
import java.io.*;
import java.util.*;

public class Tat  {

	static final String tatdoc=
	"	tat   = item*					\n"+
	"	item  = head? (neck body)? `foot		\n"+
	"	head  : graph text (nl graph text)*		\n"+
	"	neck  : vert* tab				\n"+
	"	body  : line (vert+ tab line)*			\n"+
	"	foot  : (!vert any)* vert*			\n"+
	"	text  : (sp|graph)*				\n"+
	"	line  : (tab|sp|graph)*				\n"+
	"	nl    : 10 | 13 10?				\n"+
	"	vert  : 10 | 13					\n"+
	"	tab   : 9					\n"+
	"	sp    : 32					\n"+
	"	graph : 33..1114111				\n"+
	"	any   : 1..1114111				\n";
	
	Gist tatGist;
	Prose prose;
	StringBuilder out;

	String[] tagLabels = new String[] { 
		"title","h1",    "sect","h2",     "sub","h3",
		"end","div" };

	Map<String,String> tagNames = new HashMap<String,String>();
	
	Tat() { 
		tatGist=new Gist(tatdoc,this);
		prose=new Prose();
		for (int i=0; i<tagLabels.length; i+=2) tagNames.put(tagLabels[i],tagLabels[i+1]);
	}
	
	public Object[] transform(String text) { return (Object[])tatGist.transform(text); }
	
	// semantic methods................

	public Object[] tat(Object[] items) {
		return items;
	}

	public Item item(Object[] xs) {
		if (xs.length==1) // head
			return new Item("p",null,prose.transform((String)xs[0]));
		else if (xs.length==2) // neck body
			return new Item("pre",null,(String)xs[1]);
		else if (xs.length==3) { // head neck body
			return new Item("div",(String)xs[0],(String)xs[2]);
		}
		//System.out.println("null item?..."+xs.length); // 0
		return null;
	}
	
	class Item {
		
		Item(String tag, String head, String body) {
			this.tag=tag;
			this.head=head;
			this.body=body;
			cls=head; // TODO symbol tags...
			id=null;
			if (head!=null) {
				int i=head.indexOf('#');
				if (i<0) cls=head;
				else { // name#id
					cls=head.substring(0,i);
					id=head.substring(i+1);
				}
			}
			String tagName=tagNames.get(cls);       // sect => h2
			if (tagName!=null) this.tag=tagName;    // div = h2
		}
		
		String tag, head, body, cls, id;
		
		String atts() {
			if (cls==null && id==null) return "";
			String att="";
			if (cls!=null) att+= " class='"+cls+"'";
			if (id!=null) att+= " id='"+id+"'";
			return att;
		}

		public String toString() {
			return String.format("<%s%s>\t%s</%s>%n",tag,atts(),body,tag);
		}
	}
	



	
	//void transform(String text) { tatGist.transform(text); }
	// 
	// public Object tat(Object[] xs) {
	// 	System.out.println("---	done...");
	// 	return null;
	// }
	// 
	// public Object item(Object[] xs) {
	// 	if (xs.length==1) // head
	// 		para("anon",prose.transform((String)xs[0]));
	// 	else if (xs.length==2) // neck body
	// 		pre("anon",(String)xs[1]);
	// 	else if (xs.length==3) { // head neck body
	// 		String head=(String)xs[0];
	// 		String body=(String)xs[2];
	// 		if (head.startsWith("title")) elem("h1",head,body);
	// 		else if (head.startsWith("sect")) elem("h2",head,body);
	// 		else if (head.startsWith("sub")) elem("h3",head,body);
	// 		else div(head,(String)xs[2]);
	// 	}
	// 	return null;
	// }
	
	// void para(String tag,String para) { out.append(String.format("<p %s>%s</p>%n",atts(tag),para)); }
	// 
	// void pre(String tag,String body) { out.append(String.format("<pre %s>\t%s</pre>%n",atts(tag),body)); }
	// 
	// void div(String tag,String body) { out.append(String.format("<div %s>\t%s</div>%n",atts(tag),body)); }
	// 
	// void elem(String tag,String head,String body) { out.append(String.format("<%s %s>\t%s</%s>%n",tag,atts(head),body,tag)); }
	// 
	// 
	// String atts(String tag) {
	// 	int i=tag.indexOf('#');
	// 	if (i<0) return "class='"+tag+"'";
	// 	return "class='"+tag.substring(0,i)+"' id='"+tag.substring(i+1)+"'";
	// }
	
	// -- HTML -----------------------------------------------------------------------------------

	String translate(String text) {
		Object[] items=transform(text);
		Map<String,Item> itemIds=new HashMap<String,Item>();
		for (Object item:items) if (item!=null) {
			Item it=(Item)item;
			if (it.id!=null) itemIds.put(it.id,it);
		}
		//System.out.println("ids="+itemIds.size());
		out=new StringBuilder();
		out.append("<html>\n<head>\n");
		out.append("<link rel='stylesheet' href='./tat.css' type='text/css' />\n");
		out.append("</head><body>\n");
		for (Object item:items) if (item!=null) out.append(item.toString());
		out.append("</body>\n</html>\n");
		return out.toString();
	}
	
	// file access -------------------------------------------------------------------
	
	static String readFile(File file) {
		StringBuilder sb=new StringBuilder();
		String ln;
		try {
			BufferedReader in=new BufferedReader(new FileReader(file));
			while ((ln=in.readLine())!=null) sb.append(ln).append("\n");
		} catch (IOException e) { System.out.println(e); System.exit(-1); }
		return sb.toString();
	}

	static void writeFile(File file,String text) {
		try {
			PrintWriter out= new PrintWriter(new FileWriter(file));
			out.print(text);
			out.close();
		} catch (IOException e) { System.out.println(e); System.exit(-1); }
	}

	static File findFile(String file) {
		// check for abs/rel file name... or url...  TODO
		return new File(System.getProperty("user.dir"),file);
	}

	// -- main -----------------------------------------------------------------------
	
	public static void main(String[] args) {
		Tat tat=new Tat();
		// System.out.println(tat.tatGist.rulCode());
		for (String arg:args) tatFile(tat,findFile(arg));
	}
	
	static void tatFile(Tat tat, File file) {
		System.out.println("--- "+file.getPath());
		String text=readFile(file);
		// System.out.println(text);
		String html=tat.translate(text);
		// System.out.println(html);
		File outFile=new File(file.getPath()+".html");
		writeFile(outFile,html);
	}

	// static void tatFile(File file) {
	// 	System.out.println("--- "+file.getPath());
	// 	String text=readFile(file);
	// 	// System.out.println(text);
	// 	Tat tat=new Tat();
	// 	// System.out.println(tat.tatGist.parser());
	// 	String html=tat.translate(text);
	// 	// System.out.println(html);
	// 	File outFile=new File(file.getPath()+".html");
	// 	writeFile(outFile,html);
	// }

} // Tat
	

