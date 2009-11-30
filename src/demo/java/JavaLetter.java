
public class JavaLetter {
	
	public static void main(String[] args) {
		
		int n=-1,m=-1;
		for (int i=0;i<0x10ffff;i++) {
			if (Character.isSpaceChar(i)) {
				//if (Character.isJavaIdentifierStart(i)) {
				//if (Character.isWhitespace(i)) {
				m=i;
				if (n<0) n=i;
			} else {
				if (n>=0) {
					System.out.println(n+".."+m+"  "+Integer.toHexString(n)+".."+Integer.toHexString(m));
					n=-1;
					m=-1;
				}
			}
		}
		if (n>=0) System.out.println(n+".."+m);
	}
}
