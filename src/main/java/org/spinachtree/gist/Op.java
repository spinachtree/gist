package org.spinachtree.gist;

/*
	Op is a parser operation that implements the parse() method predicate.
	
	Ops are built into a tree with And and OR Op arguments. The And Op is invoked
	if the Op's match() method succeeds, and the Or Op is called if it fails.
	
	Null And => true (the Op is all done). The And links represents a sequence.
	Null Or  => false (the Op has faild and has no other option to try). Or is a choice.
	
	An Op represents a parser expression:  (Op, And / Or)
	
	The parent Op class contains the logic to traverse And sequences and  Or choices.
	
	The bulk of the work is done by two Op subclasses:
		ref	-- a rule name reference, invokes the Op.parse() for a rule.
		chs	-- match next input character into a set of char code ranges.
	
	Parse tree building and left recursion is handled in the Rule Op.
	
	The input string and parse tree are contained in the Parser object.
		
*/
	
class Op {
	
	Parser par; // the parser contains the input string and parse tree result.

	Op And=null; // sequence link...  x , y , z , ...
	Op Or=null;  // choice link...    x / y / z / ...

	// OrMe will fail and re-parse Me, which can be very expensive (unless memo)...
	// eg for rule:  R = x R / x  (ie x+) the use of OrMe (or a memo) is vital.
	boolean OrMe=false;  

	// Rep is a repeat loop...  x*
	boolean Rep=false;
	int min=0, max=Integer.MAX_VALUE;
	
	Op and(Op x) {
		if (x==null) return this;
		if (And!=null) fault("todo: group: "+this+".and("+x+")");
		if (Or==null && notAnd(x)) return x;
		And=x;
		return this;
	}
	Op or(Op x) { 
		if (x==null) return this;
		if (Or!=null) fault("unreachable: "+this+".and("+x+")");
		if (And==null && merge(x)) return this;
		Or=x;
		OrMe=orMe(x);
		return this;
	}
	Op rep() { Rep=true; return this; }
	Op rep1() { Rep=true; min=1; return this; }
	Op opt() { Rep=true; max=1; return this; }
	
	boolean merge(Op x) { return false; }
	boolean except(Op x) { return false; }
	boolean notAnd(Op x) { return false; }
	boolean orMe(Op x) { return this==x; }
	
	Op copy() {
		if (And!=null || Or!=null || Rep!=false) return null;
		return copyMe(); // subclass
	}
	Op copyMe() { todo(); return null; }
	
	boolean parse() {
		int p=par.pos;
		Term t=par.tip;
		boolean result = Rep? loop() : match();
		if (result) {
			if (And==null) return true;
			if (And.parse()) return true;
			if (OrMe) return (Or.Or==null || Or.Or.parse());
		} 
		if (Or==null || OrMe) return false;
		par.reset(p,t);
		return Or.parse();
	}

	boolean loop() { // elide recursion...
		int i=par.pos;
		Term t=par.tip;
		int k=0;
		while (k<max && match()) {
			k+=1; 
			int j=par.pos;
			if (i<j) { i=j; t=par.tip; }
			else break;
		}
//System.out.println("loop: k="+k+" min="+min);
		if (k<min) return false;
		if (i<par.pos && k!=max) par.reset(i,t);
		return true;
	}

	public String toString() {
		String rep="";
		if (Rep)
			if (max==1) rep="?";
			else if (min==1) rep="+";
			else rep="*";
		if (Or==null)
			if (And==null) return me()+rep;
			else return "("+me()+rep+","+And+")";
		else if (And==null) return "("+me()+rep+"/"+Or+")";
		return "("+me()+rep+","+And+"/"+Or+")";
	}
	
	boolean match() { crash(); return false; }

	Op fault(Rule rule, Term term, String msg) { return par.fault(rule,term,msg); }
	Op fault(String msg) { return par.fault(msg); }

	String me() { return "<op>"; }
	
	void todo() { crash(" not implemented yet... "); }
	void crash() { crash(" internal fault... "); }
	void crash(String msg) { throw new IllegalStateException(msg); }
}


class Ref extends Op {
	Ref(Parser par, String name) {
		this.par=par;
		this.name=name;
		this.elide=false;
		resolveTarget(); // if possible
	}
	
	Ref(Parser par, Rule host, Rule rule, boolean elide) {
		this.par=par;
		this.host=host;
		this.elide=elide;
		this.rule=rule;
		this.name=rule.name;
	}
	
	void resolveTarget() {
		if (rule==null) rule=par.getRule(name);
		if (rule==null || rule.body==null) return;
		if (elide || (host!=null && host.term) || rule.elide) target=rule.body;
		else target=rule;
	}
	
	String name;
	boolean elide;
	Rule host, rule;
	Op target;
	
	Op copyMe() { return new Ref(par,name); }
	
	boolean orMe(Op x) { 
		if (x instanceof Ref) return name.equals(((Ref)x).name);
		return false;
	}
	
	boolean match() {
//System.out.println(name+" pos="+par.pos+" OrMe="+OrMe);
		if (target==null) {
			resolveTarget();
			if (target==null) fault("No target for Ref: "+name);
		}
		return target.parse();
	}

	String me() { return name; }
	
}

class Str extends Op {
	Str(Parser par, String str) {
		this.par=par;
		this.str=str.substring(1,str.length()-1);
	}

	String str;
	
	Op copyMe() { return new Str(par,str); }

	boolean match() {
		int cc=str.codePointAt(0);
		if (par.chr!=cc) return false;
		int i=0;
		while (i<str.length()-1) {
			i+=(cc<0x10000)? 1:2;
			cc=str.codePointAt(i);
			par.advance();
			if (par.chr!=cc) return false;
		}
		par.advance();
		return true;
	}

	String me() { return "'"+str+"'"; }
}

class Grp extends Op {
	Grp(Parser par, Op op) {
		this.par=par;
		this.op=op;
	}
	
	Op op;

	Op copyMe() { return new Grp(par,op); }

	boolean match() {
//System.out.println("Grp: "+me()+" pos="+par.pos+" OrMe="+OrMe);
		return op.parse();
	}

	String me() { return op.toString(); }
}

class Not extends Op {
	Not(Parser par,Op op) {
		this.par=par;
		arg=op;
	}

	Op arg;
	
	Op copyMe() { return new Not(par,arg); }
	
	boolean notAnd(Op y) {
		return y.except(arg);
	}

	boolean match() {
		int p=par.pos;
		Term t=par.tip;
		boolean result=arg.parse();
		par.reset(p,t);
		return !result;
	}

	String me() { return "!"+arg; }
}

class Peek extends Op {
	Peek(Parser par,Op op) {
		this.par=par;
		arg=op;
	}

	Op arg;
	
	Op copyMe() { return new Peek(par,arg); }

	boolean match() { 
		int p=par.pos;
		Term t=par.tip;
		boolean result=arg.parse();
		par.reset(p,t);
		return result;
	}

	String me() { return "&"+arg; }
}

class Prior extends Op {
	Prior(Parser par,Op op) {
		this.par=par;
		arg=op;
	}

	Op arg;
	
	Op copyMe() { return new Prior(par,arg); }

	boolean match() { todo(); return !arg.parse(); }

	String me() { return "@"+arg; }
}


class Event extends Op {

	Event(Parser par, Rule host, String name, String args) {
		// Event = '<' s name? s args? '>'
		this.par=par;
		this.host=host;
		this.name=(name==null)? "":name;
		this.args=(args==null)? "":args;
	}
	
	Rule host;
	String name;
	String args;
	
	boolean match() {
		if (par.action!=null)  
			return par.action.event(par,host.name,name,args);
		System.out.println(par.traceReport("trace "+host.name+": "+name+" "+args));
		return true;
	}
	
	String me() { return "<"+name+" "+args+">"; }
	
} // Event

class WhiteSpace extends Op {

	// ~  : (9..13/0x85/0x2028/Zs)* -- elide white-space
	// Zs : 32/0xA0/0x1680/0x180E/0x2000..200A/0x202F/0x205F/0x3000
	
	static int[] ranges={9,13, 32,32, 0x85,0x85, 0xA0,0xA0, 0x1680,0x1680, 0x180E,0x180E,
		0x2000,0x200A, 0x2028,0x2028, 0x202F,0x202F, 0x205F,0x205F, 0x3000,0x3000 };

	WhiteSpace(Parser par) { 
		this.par=par;
		ws=new Chs(par,ranges,ranges.length);
	}
	
	Chs ws;
	
	boolean match() {
		while (ws.match()) {}
		return true;
	}
	
	String me() { return "~"; }	
}

class NewLine extends Op {

	// $ match end-of-line: 13 (10/0x85)? / 10 / 0x85 / 0x2028

	NewLine(Parser par) { this.par=par; }

	boolean match() {
		int ch=par.chr;
		if (ch==10 || ch==0x85 || ch==0x2028) par.advance();
		else if (ch==13) {
			par.advance();
			ch=par.chr;
			if (ch==10 || ch==0x85) par.advance();
		}
		return true;
	}
	
	String me() { return "$"; }	
}

class True extends Op {
	True(Parser par) { this.par=par; }

	Op copyMe() { return this; }

	boolean match() { return true; }

	String me() { return "''"; }
}

class False extends Op {
	False(Parser par) {
		this.par=par;
	}

	Op copyMe() { return this; }

	boolean match() { return false; }

	String me() { return "!''"; }
}

