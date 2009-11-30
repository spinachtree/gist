
import org.spinachtree.gist.*;

public class TestChars {
	
	public static void main(String[] args) {
		
		System.out.println("Gist TestChars===============================================================");
		
		String codesTest=
		"	codes: %0..9                      \n"+
		"	even : !%1 !%3 !(%5/%7/%9) codes  \n"+
		"	odd  : !(%6/%2/%0) !%8 !%4 codes  \n"+
		"	low  : !%6..42 codes              \n"+
		"	hi   : !%0..5 codes               \n"+
		"	mid  : !%0..4 !%6..9 codes        \n"+
		"	void : !%0..100 hi                \n"+
		"	char : !%34 !%92 %x20..10ffff     \n";

		Gist codes=new Gist(codesTest);
		System.out.println(codesTest);
		System.out.println(codes.inspect());
		
		String charsTest=
		"	nat  = odd+ even.5 low* hi+            \n"+
		"	codes: '0'..'9'                        \n"+
		"	even : !'1' !'3' !('5'/'7'/'9') codes  \n"+
		"	odd  : !('6'/'2'/'0') !'8' !'4' codes  \n"+
		"	low  : !'6'..'~' codes                 \n"+
		"	hi   : !'0'..'5' codes                 \n"+
		"	mid  : !'0'..'4' !'6'..'9' codes       \n"+
		"	void : !0..100 hi                     \n"+
		"	char : !34 !92 0x20..10ffff          \n";

		Gist nums=new Gist(charsTest);
		System.out.println(charsTest);
		System.out.println(nums.inspect());
		Term t=nums.parse("97135246800569");
		System.out.println(t);
		
		
	}
		
	
}
