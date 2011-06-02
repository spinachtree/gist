
package org.spinachtree.gist;

class Parser {
	
	Parser(Op_rule[] rules) {
		this.rules=new Op_rules(rules);
	}
	
	Parser(Object[] rules) {
		this.rules=new Op_rules(rules);
	}
	
	Op_rules rules;
	
	Op op_call(String name, boolean elide) {
		return rules.op_call(name,elide);
	}
	
	Op_rule span_rule(Span span) {
		return rules.span_rule(span);
	}
	
	Span parse(String str) {
		Nib nib=new Nib(str);
		Span root=new Span(-1,0,0,null,null);
		Pen pen=new Pen(nib,root,0);
		Pen pen1=rules.start.parse(pen);
		if (pen1==null) fault("\nParse failed:",pen);
		if (pen1.pos<pen.nib.limit) fault("\nParse incomplete:",pen);
		return pen1.tree;
	}
	
	void fault(String msg, Pen pen) {
		String text=pen.nib.input;
		int limit=pen.nib.limit;
		int pos=pen.nib.max; // pos=fail
		int i=pos-1, j=pos, sol=0;
		while (i>=0) {
			int ch=text.codePointAt(i);
			if (ch==10 || ch==13) {
				if (sol==0) sol=i+1;
				if (pos-i>50) break;
			}
			i-=1;
		}
		while (j<limit) {
			int ch=text.codePointAt(j);
			if (ch==10 || ch==13) break;
			j+=1;
		}
		String line=text.substring(i+1,j).replace("\t"," ");
		int k=pos-sol;
		String cursor="";
		while (k>0) {cursor+=" "; k-=1;}
		String report=msg+String.format(" at %d of %d ... %n%s%n%s^  ",pos,limit,line,cursor);
		throw new IllegalArgumentException(report);
	}
	
	public String toString() {
		return rules.toString();
	}

} //Parser

