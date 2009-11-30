package org.spinachtree.gist;

import java.util.*;

class Ref extends ParseOp {
	
	Ref(String name, boolean elide, Rule host, Map<String,Rule>ruleMap, Rule target) {
		this.name=name;
		this.elide=elide;
		this.host=host;
		this.ruleMap=ruleMap;
		ruleOp=target;
	}
	
	String name;
	boolean elide;
	Rule host;
	Map<String, Rule> ruleMap;
	ParseOp ruleOp;
	
	boolean parse(Scan scan) {
		if (ruleOp==null) {
			Rule rule=ruleMap.get(name);
			if (rule==null || rule.body==null) // ?? why not caught as a compile fault
				throw new  UnsupportedOperationException("Undefined rule: "+name);
			if (elide || (host!=null && host.term) || rule.elide) ruleOp=rule.body;
			else ruleOp=rule;
		}
		return ruleOp.parse(scan);
	}

	public String toString() {
		return name;
	}
	
} // Ref

class Seq extends ParseOp {
	
	Seq(ParseOp[] args) {
		this.args = args;
	}
	
	ParseOp[] args;
	
	boolean parse(Scan scan) {
		for (ParseOp arg : args) { if (!arg.parse(scan)) return false;}
		return true;
	}

	public String toString() {
		String s="(";
		int len=args.length;
		for (int i=0;i<len;i++) { if (i!=0) s+=" "; s+=args[i].toString(); }
		return s+")";
	}
	
} // Seq

class Select extends ParseOp {

	Select(ParseOp[] array) {
		args = array;
for (ParseOp arg : args) if (arg==null) throw new IllegalArgumentException("null arg... ");
	}
	
	ParseOp[] args;
	
	boolean parse(Scan scan) {
		int i=scan.pos;
		Term t=scan.tip;
		for (ParseOp arg : args) {
			if (arg.parse(scan)) return true;
			scan.reset(i,t);
		}
		return false;
	}

	public String toString() {
		String s="(";
		int len=args.length;
		for (int i=0;i<len;i++) { if (i!=0) s+="/"; s+=args[i].toString(); }
		return s+")";
	}

} // Select

class Repeat extends ParseOp {
	
	Repeat(ParseOp item) {
		arg = item;
		min = 0;
		max = -1;
	}

	Repeat(ParseOp item, int n) {
		arg = item;
		min = n;
		max = -1;
	}

	Repeat(ParseOp item, int n, int m) {
		arg = item;
		min = n;
		max = m;
	}
	
	ParseOp arg;
	int min, max;
	
	boolean parse(Scan scan) {
		int i=scan.pos;
		Term t=scan.tip;
		int k=0;
		while (arg.parse(scan)) {
			k++;
			if (k==max) return true;
			int j=scan.pos;
			if (i==j) break;  // stuck
			i=j;
			t=scan.tip;
		}
		if (k<min) return false;
		scan.reset(i,t);
		return true;
	}
	
	public String toString() {
		if (min==0) {
			if (max==1) return arg+"?";
			if (max==-1) return arg+"*";
		}
		if (min==1)
			if (max==-1) return arg+"+";
		if (max==-1)
			return arg+"*"+min+".._";
		if (min==max)
			return arg+"*"+min;
		return arg+"*"+min+".."+max;
	}
	
} // Repeat

class Negate extends ParseOp {
	
	Negate(ParseOp item) {
		arg = item;
	}
	
	ParseOp arg;
	
	boolean parse(Scan scan) {
		int i=scan.pos;
		Term t=scan.tip;
		boolean ans=arg.parse(scan);
		scan.reset(i,t);
		return (!ans);
	}

	public String toString() {
		return "!"+arg;
	}
	
} // Negate

class Peek extends ParseOp {
	
	Peek(ParseOp item) {
		arg = item;
	}
	
	ParseOp arg;
	
	boolean parse(Scan scan) {
		int i=scan.pos;
		Term t=scan.tip;
		boolean ans=arg.parse(scan);
		scan.reset(i,t);
		return ans;
	}
	
	public String toString() {
		return "&"+arg;
	}
	
} // Peek

class Prior extends ParseOp {
	
	Prior(String str) {
		name = str;
	}
	
	String name;
	
	// compiler must mark host rule.fixed=false (can't use memos)
	// boolean fixed() { return false; }
	
	boolean parse(Scan scan) {
		Term p=scan.tip; // using prior link...
		while (p!=null && !p.isTag(name)) p=p.prior;
		if (p==null) return false;
		String str=p.text();
		if (!scan.input.startsWith(str,scan.pos)) return false;
		scan.pos+=str.length();
		return true;
	}
	
	public String toString() {
		return "@"+name;
	}
	
} // Prior


class Event extends ParseOp {

	Event(Term event, Rule host) {
		// Event = '<' name? s args? '>'
		this.event=event;
		rule=host.name();
		name=event.text("name");
		args=event.text("args");
	}
	
	Term event;
	String rule;
	String name;
	String args;
	
	boolean parse(Scan scan) {
		if (scan.vent!=null)  
			return scan.vent.event(scan,rule,name,args);
		System.out.println(scan.traceReport(rule+": "+event.text()));
		return true;
	}
	
	public String toString() {
		return event.text();
	}
	
} // Event

class WhiteSpace extends ParseOp {

	// ~  : (9..13/0x85/0x2028/Zs)* -- elide white-space
	// Zs : 32/0xA0/0x1680/0x180E/0x2000..200A/0x202F/0x205F/0x3000
	
	static int[] ranges={9,13, 32,32, 0x85,0x85, 0xA0,0xA0, 0x1680,0x1680, 0x180E,0x180E,
		0x2000,0x200A, 0x2028,0x2028, 0x202F,0x202F, 0x205F,0x205F, 0x3000,0x3000 };

	static Chars ws=new Chars(ranges);
	
	boolean parse(Scan scan) {
		while (ws.parse(scan)) {}
		return true;
	}
	
	public String toString() { return "~"; }
	
}

class NewLine extends ParseOp {

	// $ match end-of-line: 13 (10/0x85)? / 10 / 0x85 / 0x2028

	boolean parse(Scan scan) {
		int i=scan.pos;
		int ch=scan.input.codePointAt(i); 
		if (ch==10 || ch==0x85 || ch==0x2028) { 
			scan.pos+=1;
			return true;
		}
		if (ch==13) {
			scan.pos+=1;
			ch=scan.input.codePointAt(i+1);
			if (ch==10 || ch==0x85) scan.pos+=1;
			return true;
		}
		return true;
	}
	
	public String toString() { return "$"; }
	
}

class Empty extends ParseOp {
	
	boolean parse(Scan scan) { return true; }
	
	public String toString() { return "''"; }
	
}


