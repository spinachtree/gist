
import org.spinachtree.gist.*;

public class Shop {
	
	public static void main(String[] args) {
		
		Gist shop = new Gist(
			"lines = (~ item)* ",
			"item  = name s.. value ",
			"name  : black+ ",
			"s     : white* -- in-line tab or space chars...    ",
			"value : text* -- black/white, the rest of the line ");
		
		Term items=shop.parse("foo bar\nspam 4 tins\n\n\thoney	big pot\n");
	
		System.out.println(items);		
	}

}

