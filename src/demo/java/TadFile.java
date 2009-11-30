
import org.spinachtree.gist.*;
import java.io.*;

public class TadFile {
	
	public static void main(String[] args) {
		
		System.out.println("Tad=========================================================TadFile");
		
		// This grammar is total, it will accept any text file.
		// The Gist parser generates a tree of text and item elements.
		// A document can mix paragraphs of prose text with data items.
		
		// A paragraph of prose text can contain any characters, the only restriction is
		// that it can not start with a tag name, and it is terminated with a blank
		// line, or a line starting with a tag name, or the end of the document.
		
		// A tag name can be any word or symbol that does not contain white space,
		// but it must be followed by a tab character, or a new line inset with
		// a tab character. For convenience four or more space characters may be used
		// in place of a tab character.
		
		// A data record starts with a tag name, and can contain any inset text:
		//
		//    book
		//        title    A Good Cook Book.
		//        author
		//            name     Best Chef.
		//            email    chef@kitchen
		//        isbn    123456789
		//
		// This data record is an inset paragraph, but after the inset margin
		// is stripped off it starts with a "book" tag. The "book" contains
		// nested tag named data elemets. Each tag name determines the
		// syntax of the text that it contains.
		
		String tadDoc = // top level tag items (visual space).....
		"	doc   = (item / text / eol..)*	 		\n"+
		"	item  = tag data						\n"+
		"	tag   = key zig					    	\n"+
		"	data  = line (zig line / gap line?)*	\n"+
		"	text  : chr+ (nl.. !tag chr+)*			\n"+
		"   key   : black*							\n"+
		"	line  : chr+							\n"+
		"	zig   = nl? zag							\n"+
		"	gap   = eol* zag						\n"+
		"	zag   = tab tos* / ' ' tos+				\n"+
		"	tos   : tab / ' '						\n"+
		"	tab   : 9   							\n"+
		"	black : chr - 1..32						\n"+
		"	nl    : 13 (10/0x85)? / eol				\n"+
		"	eol   : 10/13/0x85/0x2028 				\n"+
		"	chr   : 0x1..10ffff - eol				\n";

		
        // String tadDoc = // top level tag items (visual space).....
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
        // "    chr   : 0x1..10ffff - eol               \n";

		// String tadDoc = // top level tag items (visual space).....
		// "	doc   = (item/text/~eol)*	 		\n"+
		// "	item  = tag ~tip data				\n"+
		// "	tag   : black*						\n"+
		// "	text  = line (~eol !(tag ~tip) line)*	\n"+
		// "	data  : chr+ (~tip chr+)*			\n"+
		// "	line  : chr+						\n"+
		// "	tip   : eol* tos					\n"+
		// "	tos   : tab+ / '  ' ' '+			\n"+
		// "	tab   : 9							\n"+
		// "	nl    : 13 (10/0x85)? / eol			\n"+
		// "	eol   : 10/13/0x85/0x2028 			\n"+
		// "	black : chr - 1..32					\n"+
		// "	chr   : 0x1..10ffff - eol			\n";

	// 	String tadDoc = // top level tag items (visual space).....
	// 	"	doc   = (item/text/~gap)*	 			\n"+
	// 	"	item  = tag ~tip data					\n"+
	// 	"	text  = chr+ (~eol !(tag ~tip) chr+)*	\n"+
	// 	"	data  : chr+ (~tip chr+)*				\n"+
	// 	"	tag   : (!%0..32 chr)*				\n"+
	// 	"	tip   : gap* tos						\n"+
	// 	"	tos   : tab+ / ' '*3.._				\n"+
	// 	"	tab   : 9							\n"+
	// 	"	eol   : 13 (10/0x85)? / gap			\n"+
	// 	"	gap   : 10/13/0x85/0x2028 			\n"+
	// 	"	chr   : !gap 0x0..10ffff				\n";
	// 
	// String tadDoc = // top level tag items (visual space).....
	// "	doc     = (item/text/gap)*	 			\n"+
	// "	item    = tag tip data					\n"+
	// "	text    = chr+ (eol !(tag tip) chr+)*	\n"+
	// "	data    : chr+ (tip chr+)*				\n"+
	// "	tag     : (!%0..32 chr)*				\n"+
	// "	tip..   : gap* tos						\n"+
	// "	tos..   : tab+ / ' '*4..				\n"+
	// "	tab..   : 9							\n"+
	// "	eol..   : 13 (10/0x85)? / gap			\n"+
	// "	gap..   : 10/13/0x85/0x2028 			\n"+
	// "	chr..   : !gap 0x0..10ffff				\n";

    // String tadDocNumeric = // top level tag items (visual space).....
    // "    doc     = (item/text/gap)*              \n"+
    // "    item    = tag tie~ data                 \n"+
    // "    text    = chr+ (eol !(tag tie~) chr+)*  \n"+
    // "    data    : chr+ (gap* tos~ chr+)*            \n"+
    // "    tag     : graph*                        \n"+
    // "    tie~    : gap* tos                      \n"+
    // "    tos~    : tab+ / ' ' ' '+               \n"+
    // "    tab~    : 9                             \n"+
    // "    graph~  : 33..0x10ffff                  \n"+
    // "    eol~    : 13 (10/0x85)? / gap           \n"+
    // "    gap~    : 10/13/0x85/0x2028             \n"+
    // "    chr~    : !gap 0..0x10ffff              \n";
    // 
    //  String tadFile= // top level tag items (canonical tab).....
    //  "   doc     = (item/text/gap)*          \n"+
    //  "   item    = tag gap* tab+ data        \n"+
    //  "   text    = chr+ (eol !isit chr+)*    \n"+
    //  "   isit    : tag gap* tab              \n"+
    //  "   data    : chr+ (gap* tab chr+)*     \n"+
    //  "   tag     : (!%0..32 chr)*            \n"+
    //  "   tab..   : %9                        \n"+
    //  "   eol..   : %13 gap? / gap            \n"+
    //  "   gap..   : %10/%13/%x85/%x2028       \n"+
    //  "   chr..   : !gap %x0..10ffff          \n";
    // 
    //  String tadItem= // parse an item with nested tad content.....
    //  "   item    = tags gap+ @margin data    \n"+
    //  "   text    = chr+ (eol !isit chr+)*        \n"+
    //  "   isit    : @margin tag (gap+ @margin)? tab               \n"+
    //  "   data    : chr+ (gap* @margin tab chr+)*     \n"+
    //  "   tags    = tag (tab+ tag)*           \n"+
    //  "   tag     : (!%0..32 chr)*            \n"+
    //  "   margin  : tab*                      \n"+
    //  "   tab..   : %9                        \n"+
    //  "   eol..   : %13 gap? / gap            \n"+
    //  "   gap..   : %10/%13/%x85/%x2028       \n"+
    //  "   chr..   : !gap %x0..10ffff          \n";


		// String tadFile= // top level tag items.....
		// "	doc 	= (item/text/gap)*	 		\n"+
		// "	item	= margin tag go data		\n"+
		// "	text    = margin para					\n"+
		// "	data    : chr+ (gap* @margin tab chr+)*					\n"+
		// "	para    : chr+ (eol @margin !(tag go) chr+)*					\n"+
		// "	go..      : tab / eol @margin tab									\n"+
		// "	tag     : (!%0..32 chr)+									\n"+
		// "	margin  : tab*									\n"+
		// "	tab..   : %9									\n"+
		// "	eol..   : %13 gap? / gap						\n"+
		// "	gap..   : %10/%13/%x85/%x2028 					\n"+
		// "	chr..   : !gap %x0..10ffff	\n";

        // String tadFile= // fully nested tree.....
        // "   doc     = data*                                 \n"+
        // "   data    = gap.. margin (item/text/nl..)*           \n"+
        // "   item    = tag (tab+ (item/text) / &inset data)  \n"+
        // "   text    : chr+ (nl.. chr+ !tab)*                  \n"+
        // "   tag     : chr+                                  \n"+
        // "   margin  : tab*                                  \n"+
        // "   inset   : gap @margin tab                      \n"+
        // "   nl      : eol @margin                           \n"+
        // "   tab     : %9                                    \n"+
        // "   eol..   : %13 gap? / gap                        \n"+
        // "   gap     : xxol*                   \n"+
        // "   gap..   : %10/%13/%x85/%x2028                   \n"+
        // "   chr..   : %x20..84/%x86..2027/%x2029..10ffff    \n";

        // Gist tad=new Gist( // top level tag items (visual space).....
        // "    doc   = (item / text / eol..)*          \n"+
        // "    item  = tag data                        \n"+
        // "    tag   = key zig                         \n"+
        // "    text  : chr+ (nl !tag chr+)*            \n"+
        // "    data  = line (zig line / gap line?)*    \n"+
        // "   key   : black*                           \n"+
        // "    line  = head (neck tail)?               \n"+
        // "    neck  : tab+ fold? / fold               \n"+
        // "    head  : field                       \n"+
        // "    tail  = field ((zag/fold) field)*                       \n"+
        // "    field : (chr-tab)+                          \n"+
        // "    fold  : nl? tab tab+                            \n"+
        // "    zig   : nl? zag                         \n"+
        // "    gap   : eol* zag                        \n"+
        // "    zag   : tab tos* / ' ' tos+             \n"+
        // "    tos   : tab / ' '                       \n"+
        // "    tab   : 9                               \n"+
        // "    black : chr - 1..32                     \n"+
        // "    nl    : 13 (10/0x85)? / eol             \n"+
        // "    eol   : 10/13/0x85/0x2028               \n"+
        // "    chr   : 0x1..10ffff - eol               \n");

    // Gist tad=new Gist( // top level tag items (visual space).....
    // "    doc   = (item / text / eol..)*          \n"+
    // "    item  = tag data                        \n"+
    // "    tag   = key zig                         \n"+
    // "    text  : chr+ (nl !tag chr+)*            \n"+
    // "    data  = line (zig line / gap line?)*    \n"+
    // "   key   : black*                           \n"+
    // "    line  = head (neck tail)?               \n"+
    // "    neck  : tab+ fold? / fold               \n"+
    // "    head  : field                       \n"+
    // "    tail  = field ((zag/fold) field)*                       \n"+
    // "    field : (chr-tab)+                          \n"+
    // "    fold  : nl? tab tab+                            \n"+
    // "    zig   : nl? zag                         \n"+
    // "    gap   : eol* zag                        \n"+
    // "    zag   : tab tos* / ' ' tos+             \n"+
    // "    tos   : tab / ' '                       \n"+
    // "    tab   : 9                               \n"+
    // "    black : chr - 1..32                     \n"+
    // "    nl    : 13 (10/0x85)? / eol             \n"+
    // "    eol   : 10/13/0x85/0x2028               \n"+
    // "    chr   : 0x1..10ffff - eol               \n");

Gist tad=new Gist( // top level tag items (visual space).....
"	doc   = (item / text / eol..)*	 		\n"+
"	item  = tag data						\n"+
"	tag   = key zig.. 						\n"+
"	text  : chr+ (nl !tag chr+)*			\n"+
"	data  = line (zig.. line / gap line?)*	\n"+
"   key   : black*							\n"+
"	line  = head (neck.. tail)?				\n"+
"	neck  : tab+ fold? / fold				\n"+
"	head  : field						\n"+
"	tail  : field ((zag/fold) field)*						\n"+
"	field : (chr-tab)+							\n"+
"	fold  : nl? tab tab+							\n"+
"	zig   : nl? zag							\n"+
"	gap   : eol* zag						\n"+
"	zag   : tab tos* / ' ' tos+				\n"+
"	tos   : tab / ' '						\n"+
"	tab   : 9   							\n"+
"	black : chr - 1..32						\n"+
"	nl    : 13 (10/0x85)? / eol				\n"+
"	eol   : 10/13/0x85/0x2028 				\n"+
"	chr   : 0x1..10ffff - eol				\n");

	//	Gist tad=new Gist(tadFile);
		//System.out.println(tadDoc);
		System.out.println(tad.inspect());
		// Term doc=tad.parse("First a para\nof a few lines\n of text\nbook\n\ttitle\tThe Cook Book.\n\tisbn\t1234\nOther\tstuff..");
		// System.out.println(doc);

		try {
			Reader reader= new FileReader("doc/GistJava.txt");
			String src= read(reader);
			System.out.println(src);
			Term doc=tad.parse(src);
			System.out.println(doc);
		} catch (IOException e) { 
			System.out.println(e);
			System.exit(-1);
		}

		System.out.println("-------------------------------------");
		
		// Gist tadBlock=new Gist(tadItem);
		// System.out.println(tadBlock.inspect());
		
		// Term in1=tadBlock.parse("\ttitle\tThe Cook Book.\n\tisbn\t1234");
		// System.out.println(in1);
		
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
