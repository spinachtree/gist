
import org.spinachtree.gist.*;

public class Play {
	
	public static void main(String[] args) {
		
		Gist csv = new Gist(
		"file    = record (CRLF record)* CRLF?",
		"record  = field (COMMA field)*",
		"field   = escaped / non_esc ",
		"non_esc : TEXTDATA*",
		"escaped : DQUOTE (TEXTDATA / COMMA / CRLF / DQUOTE DQUOTE)* DQUOTE",
		"DQUOTE  : 0x22",
		"COMMA 	: 0x2C",
		"CRLF 	: 0xD 0xA",
		"TEXTDATA: 0x20..21 / 0x23..2B / 0x2D..7E");

		Term tree=csv.parse("One,Two,Three\r\n"+
		"Length x,\"Area x,y\",\"Volume \"\"x,y,z\"\"\"\r\n");
	
		System.out.println(tree);


		Gist csv1 = new Gist(
		"file    = record (nl record)*",
		"record  = field (',' field)*",
		"field   = chars / escaped",
		"chars   : (text-quote-',')*", 
		"escaped = quote (text-quote / quote quote / nl)* quote");

		System.out.println(csv1.inspect());
		
		Term tree1=csv1.parse("One,Two,Three\n"+
		"Length x,\"Area x,y\",\"Volume \"\"x,y,z\"\"\"");
		
		System.out.println(tree1);
		

		Gist list1 = new Gist(" list = int (',' int)* ");

		Term nums=list1.parse("1,2,3,4");
		
		System.out.println(nums);

		Gist list2 = new Gist(
			"list = int ',' list / int",
			"int  :  '0'..'9'+  ");

		nums=list2.parse("1,2,3,4");
		
		System.out.println(nums);

		Gist list3 = new Gist(
			"list = list ',' int / int",
			"int  :  '0'..'9'+  ");

		nums=list3.parse("1,2,3,4");
		
		System.out.println(nums);
		
		Gist list4 = new Gist(
			"list = list ',' list / int",
			"int  :  '0'..'9'+  ");

		nums=list4.parse("1,2,3,4");
		
		System.out.println(nums);

	}

}


