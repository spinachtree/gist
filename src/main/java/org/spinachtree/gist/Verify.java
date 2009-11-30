package org.spinachtree.gist;

class Verify {
	
	// Check for faults:
	// 	A/B if A can be an empty match, or vacant eg A* => can't reach B
	//	A* B if A covers B, eg A==B or A isa prefix of B => can't reach B
	
	// A very small part of this is implemented, but enough to reject some obvious faults...
	
	// vacant method tries to determine if a expression has an empty match....
	
	static boolean vacant(ParseOp x) {
		if (x==null) return false;
		if (x instanceof Chars) return false;
		if (x instanceof Repeat) {
			Repeat rep=(Repeat)x;
			if (rep.min==0) return true;
			return vacant(rep.arg);
		}
		if (x instanceof Seq) {
			Seq seq=(Seq)x;
			for (ParseOp y:seq.args) if (!vacant(y)) return false;
			return true;
		}
		if (x instanceof Select) {
			Select sel=(Select)x;
			for (ParseOp y:sel.args) if (vacant(y)) return true;
			return false;
		}
		if (x instanceof Ref) {
			Ref ref=(Ref)x;
			ParseOp rule=ref.ruleOp;
			if (rule==null) // ref loop..
				return false;
			return vacant(((Rule)rule).body);
		}
		if (x instanceof Prior) return false;
		if (x instanceof WhiteSpace) return true;
		if (x instanceof NewLine) return false;
		if (x instanceof Empty) return true;

		// Not known... => false, benefit of the doubt when undecided...
		
		if (x instanceof Negate) return false; // maybe
		if (x instanceof Peek) return false; // maybe
		if (x instanceof Event) return false; // maybe

		//throw new GistFault("missing vacant case...");
		return false; // benefit of the doubt, not proven!
	}
	
	// if a overruns b, then a* b will fail...
	
	// current implementation will only catch a few obvious cases, but everything helps...
	
	static boolean overruns(ParseOp a, ParseOp b) {
		if (a instanceof Repeat) {
			Repeat as=(Repeat)a;
			Chars x=charsOf(as.arg);
			if (x!=null) { // x* y
				Chars y=firstChars(b);
				if (y==null) return false; // can't tell...
				return covers(x,y);
			}
		}
		return false; // benefit of doubt, can't tell...
	}
	
	static Chars charsOf(ParseOp x) {
		if (x==null) return null;
		if (x instanceof Chars) return (Chars)x;
		if (x instanceof Ref) return charsOf(((Ref)x).ruleOp);
		if (x instanceof Rule) return charsOf(((Rule)x).body);
 		return null;
	}
	
	static boolean covers(Chars a, Chars b) {
		Chars x=b.exclude(a);
		if (x.size==0) return true;
		return false;
	}
	
	static Chars firstChars(ParseOp x) {
		if (x instanceof Chars) return (Chars)x;
		if (x instanceof Repeat) {
			Repeat rep=(Repeat)x;
			return firstChars(rep.arg);
		}
		if (x instanceof Seq) {
			Seq seq=(Seq)x;
			return firstChars(seq.args[0]);
		}
		if (x instanceof Select) {
			Select sel=(Select)x;
			Chars cs=null;
			for (ParseOp y:sel.args) {
				Chars cy=firstChars(y);
				if (cy==null) return null;
				if (cs==null) cs=cy;
				else cs=cs.union(cy);
			}
			return cs;
		}
		if (x instanceof Ref) {
			Ref ref=(Ref)x;
			ParseOp rule=ref.ruleOp;
			if (rule==null) return null; // ref loop..
			return firstChars(((Rule)rule).body);
		}
		return null; // can't tell...
	}
	
	// if exclusive(a,b) then a/b must be safe....
	
	// static boolean exclusive(Chars a, Chars b) {
	// 		int[] ra = a.ranges;
	// 		int[] rb = b.ranges;
	// 		int i=0, j=0;
	// 		while (i<ra.length && j<rb.length) {
	// 			int a1=ra[i], a2=ra[i+1];
	// 			int b1=rb[j], b2=rb[j+1];
	// 			if      (a2<b1) i+=2;
	// 			else if (b2<a1) j+=2;
	// 			else return false;
	// 		}
	// 		return true;
	// 	}
	
} // Verify

