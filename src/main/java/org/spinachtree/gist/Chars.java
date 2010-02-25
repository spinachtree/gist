package org.spinachtree.gist;

class Chars extends ParseOp {
	
	Chars(int[] ranges, int size) {
		if (size%2!=0) badChars(ranges);
		int i=0; // verify ordered...
		while (i<size) {
			if (ranges[i]>ranges[i+1]) badChars(ranges);
			if (i>0 && ranges[i-1]>=ranges[i]) badChars(ranges);
			i+=2; // ranges=[i,j,k,l,...] i<=j < k<=l < ...
		}
		this.ranges=ranges;
		this.size=size;
	}
	
	Chars(int[] ranges) {
		this(ranges,ranges.length);
	}

	int[] ranges; // [min,max,...] ordered code point ranges
	int size;     // count of ranges: in 0..ranges.length, size%2==0
	
	boolean parse(Scan scan) {
		int i=scan.pos;
		if (i>=scan.eot) return false;
		int ch=scan.codePoint();
		int bot=0;
		int top=size;
		while (bot<top) {
			int pick=bot+((top-bot)>>>2<<1);
			if (ch<ranges[pick])
				top=pick;
			else if (ch>ranges[pick+1])
				bot=pick+2;
				else { // ch is in range... advance..
					scan.advance();
					return true;
				}
		}
		return false;
	}

	Chars union(Chars other) {
		int[] r1=ranges, r2=other.ranges;
		int i1=0, i2=0, t1=size, t2=other.size;
		Chars result=new Chars(new int[t1+t2],0); // empty max length
		while (i1<t1 && i2<t2) {
			int x1=r1[i1], x2=r1[i1+1]; // x1..x2   
			int y1=r2[i2], y2=r2[i2+1]; // y1..y2
			if      (x2<y1-1)          { result.add(x1,x2); i1+=2; }        // xxx yyy
			else if (y2<x1-1)          { result.add(y1,y2); i2+=2; }        // yyy xxx
			else if (x1<=y1 && x2>=y2) { result.add(x1,x2); i1+=2; i2+=2; } // xxx yyy xxx
			else if (y1<=x1 && y2>=x2) { result.add(y1,y2); i1+=2; i2+=2; } // yyy xxx yyy 
			else if (x1<=y1 && y2>=x2) { result.add(x1,y2); i1+=2; i2+=2; } // xxx yyy
			else if (y1<=x1 && x2>=y2) { result.add(y1,x2); i1+=2; i2+=2; } // yyy xxx
			else throw new IllegalStateException("Woops...??");
		}
		while (i1<t1) result.add(r1[i1++],r1[i1++]);
		while (i2<t2) result.add(r2[i2++],r2[i2++]);
		return result;
	}
	
	Chars exclude(Chars other) {
		int[] r1=ranges, r2=other.ranges;
		int i1=0, i2=0, t1=size, t2=other.size;
		int x1x=0; // min for a partial reminant
		Chars result=new Chars(new int[t1+t2],0); // empty max length
		while (i1<t1 && i2<t2) {
			int x1=r1[i1], x2=r1[i1+1]; // x1..x2 = |x1+++++x2| current range
			int y1=r2[i2], y2=r2[i2+1]; // y1..y2 = |y1-----y2| delete range
			if (x1x>0) { x1=x1x; x1x=0; } // partial reminant
			if (x2<y1) { result.add(x1,x2); i1+=2; }                            // |+++| |---|
			else if (y2<x1) i2+=2;                                              // |---| |+++|
			else if (y1<=x1 && y2>=x2)  i1+=2;                                  // |--|++|--|
			else if (y1<=x1 && y2<x2) { x1x=y2+1; i2+=2; }                      // |---|+++|
			else if (x1<y1 && y2>=x2) { result.add(x1,y1-1); i1+=2; }           // |+++|---|
			else if (x1<y1 && x2>y2) { result.add(x1,y1-1); x1x=y2+1; i2+=2; }  // |+++|---|+++|
			else throw new IllegalStateException("Woops...??");
		}
		while (i1<t1) {
			int x1=r1[i1++], x2=r1[i1++];
			if (x1x>0) { x1=x1x; x1x=0; } // partial reminant
			result.add(x1,x2);
		}
		return result;
	}
	
	void add(int i, int j) {
		if (size>0 && i-1 <= ranges[size-1]) {
			ranges[size-1]=j; // merge: a..b / i..j => a..j
			if (j<ranges[size-2]) badChars(ranges);
		} else { // regular add:  a..b / i..j
			ranges[size++]=i; ranges[size++]=j;
			if (i>j) badChars(ranges);
		}
	}

	void badChars(int[] ranges) {
		String rs="";
		for (int i: ranges) rs+=String.valueOf(i)+" ";
		throw new IllegalStateException("Bad Chars range: "+rs);
	}

	public String toString() {
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

} // Chars

