package org.spinachtree.gist;

class Chs extends Op {
	
	Chs(Parser par, String s1, String s2) {
		this(par,s1,s2,10);
	}
	
	Chs(Parser par, String s1, String s2, int radix) {
		this.par=par;
		if (s2==null) s2=s1;
		if (s1.substring(0,1).equals("'")) 
			ranges=new int[] {s1.codePointAt(1), s2.codePointAt(1)};
		else ranges=new int[] {Integer.parseInt(s1,radix), Integer.parseInt(s2,radix)};
		size=ranges.length;
		if (ranges[0]>ranges[1]) fault("Bad range: "+s1+".."+s2);
 	}

	Chs(Parser par, int[] ranges, int size) {
		this.par=par;
		this.ranges=ranges;
		this.size=size;
		verify();
	}
	
	int[] ranges; // [min,max,...] ordered code point ranges
	int size;     // count of ranges: in 0..ranges.length, size%2==0
	
	Op copyMe() { return new Chs(par,ranges,size); }

	boolean orMe(Op x) { 
		if (x instanceof Chs) return same((Chs)x); else return false;
	}

	boolean same(Chs x) {
		if (x.size!=size) return false;
		for (int i=0; i<size; i++) {
			if (x.ranges[i]!=ranges[i]) return false;
		}
		return true;
	}

	boolean match() {
//System.out.println(me()+" pos="+par.pos+" OrMe="+OrMe);
//		par.chs++;
		int ch=par.chr;
		int bot=0;
		int top=size;
		while (bot<top) {
			int pick=bot+((top-bot)>>>2<<1);
			if (ch<ranges[pick]) top=pick;
			else if (ch>ranges[pick+1]) bot=pick+2;
			else { // ch is in range... advance..
				par.advance();
				return true;
			}
		}
		return false;
	}

	boolean merge(Op op) {
		if (!(op instanceof Chs)) return false;
		Chs other=(Chs)op;
		int[] r1=ranges, r2=other.ranges;
		int i1=0, i2=0, t1=size, t2=other.size, k=0;
		result=new int[t1+t2]; // empty max length, size=k
		while (i1<t1 && i2<t2) {
			int x1=r1[i1], x2=r1[i1+1]; // x1..x2   
			int y1=r2[i2], y2=r2[i2+1]; // y1..y2
			if      (x2<y1-1)          { k=add(x1,x2,k); i1+=2; }        // xxx yyy
			else if (y2<x1-1)          { k=add(y1,y2,k); i2+=2; }        // yyy xxx
			else if (x1<=y1 && x2>=y2) { k=add(x1,x2,k); i1+=2; i2+=2; } // xxx yyy xxx
			else if (y1<=x1 && y2>=x2) { k=add(y1,y2,k); i1+=2; i2+=2; } // yyy xxx yyy 
			else if (x1<=y1 && y2>=x2) { k=add(x1,y2,k); i1+=2; i2+=2; } // xxx yyy
			else if (y1<=x1 && x2>=y2) { k=add(y1,x2,k); i1+=2; i2+=2; } // yyy xxx
			else throw new IllegalStateException("Woops...??");
		}
		while (i1<t1) k=add(r1[i1++],r1[i1++],k);
		while (i2<t2) k=add(r2[i2++],r2[i2++],k);
		ranges=result;
		size=k;
		return true;
	}
	
	int[] result;

	int add(int i, int j, int k) {
		if (k>0 && i-1 <= result[k-1]) {
			result[k-1]=j; // merge: a..b / i..j => a..j
			//if (j<result[k-2]) crash();
		} else { // regular add:  a..b / i..j
			result[k++]=i; result[k++]=j;
			//if (i>j) crash();
		}
		return k;
	}

	boolean except(Op op) {
		if (!(op instanceof Chs)) return false;
		Chs other=(Chs)op;
		int[] r1=ranges, r2=other.ranges;
		int i1=0, i2=0, t1=size, t2=other.size, k=0;
		int x1x=0; // min for a partial reminant
		result=new int[t1+t2]; // empty max length, size=k
		while (i1<t1 && i2<t2) {
			int x1=r1[i1], x2=r1[i1+1]; // x1..x2 = |x1+++++x2| current range
			int y1=r2[i2], y2=r2[i2+1]; // y1..y2 = |y1-----y2| delete range
			if (x1x>0) { x1=x1x; x1x=0; } // partial reminant
			if (x2<y1) { k=add(x1,x2,k); i1+=2; }                            // |+++| |---|
			else if (y2<x1) i2+=2;                                           // |---| |+++|
			else if (y1<=x1 && y2>=x2)  i1+=2;                               // |--|++|--|
			else if (y1<=x1 && y2<x2) { x1x=y2+1; i2+=2; }                   // |---|+++|
			else if (x1<y1 && y2>=x2) { k=add(x1,y1-1,k); i1+=2; }           // |+++|---|
			else if (x1<y1 && x2>y2) { k=add(x1,y1-1,k); x1x=y2+1; i2+=2; }  // |+++|---|+++|
			else throw new IllegalStateException("Woops...??");
		}
		while (i1<t1) {
			int x1=r1[i1++], x2=r1[i1++];
			if (x1x>0) { x1=x1x; x1x=0; } // partial reminant
			k=add(x1,x2,k);
		}
		ranges=result;
		size=k;
		return true;
	}

	void verify() {
		if (size%2!=0) fault();
		int i=0; // verify ordered...
		while (i<size) {
			if (ranges[i]>ranges[i+1]) fault();
			if (i>0 && ranges[i-1]>=ranges[i]) fault();
			i+=2; // ranges=[i,j,k,l,...] i<=j < k<=l < ...
		}
	}

	void fault() {
		String rs="";
		for (int i: ranges) rs+=String.valueOf(i)+" ";
		fault("Bad char code range: "+rs);
	}

	String me() {
		String s="";
		int i=0;
		if (size>2) s+="(";
		while (i<size) {
			if (i>0) s+="/";
			int n=ranges[i], m=ranges[i+1];
			if (n==m) { s+=String.valueOf(n); }
			else { s+=String.valueOf(n)+".."+String.valueOf(m); }
			i+=2;
		}
		if (size>2) s+=")";
		return s;
	}

} // Chs

