
import org.spinachtree.gist.*;

public class CharTest {
	
	public static void main(String[] args) {
		
		System.out.println("Gist CharTest==========================");
		
		String codesTest=
		"	codes: 0..9                       \n"+
		"	gap  : Zgap ; @import gist.pragma                         \n"+
		"	gap1 : 10/11/12/13/14             \n"+
		"	gap2 : 10..12                     \n"+
		"	even : !1 !3 !(5/7/9) codes       \n"+
		"	odd  : !(6/2/0) !8 !4 codes       \n"+
		"	low  : !6..42 codes               \n"+
		"	hi   : !0..5 codes                \n"+
		"	mid  : !0..4 !6..9 codes          \n"+
		"	void : !0..100 hi                 \n"+
		"	char : !34 !92 0x20..10ffff       \n";

		Gist codes=new Gist(codesTest);
		System.out.println(codesTest);
		System.out.println(codes.inspect());
		
	}
		
	
}
