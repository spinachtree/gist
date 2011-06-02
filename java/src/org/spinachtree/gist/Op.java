
package org.spinachtree.gist;

import java.util.*;

class Op {  // grammar operators....
	
	Op compose(Op_rules rules, boolean elide) {
		fault(String.format("compose not implemented for: %s%n",this.getClass().getName()));
		return this;
	}

	Pen parse(Pen p) {
		fault("parse not implemented...");
		return null; // fail
	}
	
	public String toString() {
		return "Op(?)";
	}
	
	boolean isChs() {
		return false;
	}

	boolean isNotChs() {
		return false;
	}
	
	Op_chs getNotChs() {
		fault("woops...");
		return null;
	}
	
	void fault(String msg) {
		throw new IllegalArgumentException(msg);
	}
}

class Op_rules extends Op { // grammar rules....
	
	Op_rules(Op_rule[] rules) {
		this.rules=rules;
		rule_idx=new HashMap<String,Integer>(); 
		for (int i=0;i<rules.length;i++) {
			String name=rules[i].name;
			if (rule_idx.get(name)!=null) fault("duplicate rule: "+name);
			rule_idx.put(name,new Integer(i));
		}
		compose_rules();
	}

	Op_rules(Object[] rules) {
		this.rules=new Op_rule[rules.length];
		rule_idx=new HashMap<String,Integer>(); 
		for (int i=0;i<rules.length;i++) {
			Op_rule rule=(Op_rule)rules[i];
			this.rules[i]=rule;
			if (rule_idx.get(rule.name)!=null) fault("duplicate rule: "+rule.name);
			rule_idx.put(rule.name,new Integer(i));
		}
		compose_rules();
	}

	void compose_rules() {
		rule_map=new HashMap<String,Op_rule>(); 
		for (Op_rule rule : rules) rule_map.put(rule.name,rule);
		// compose links references to rules...
		for (Op_rule rule : rules) rule_map.get(rule.name).compose(this);
		// start rule...
		start=op_call(rules[0].name);
	}
	
	Op_rule[] rules;
	Map<String,Integer> rule_idx;
	Map<String,Op_rule> rule_map;
	Op start;

	Op op_call(String name) {
		return op_call(name,false);
	}
	Op op_call(String name, boolean elide) {
		if (rule_idx.get(name)==null) return null; // no such rule
		return new Op_call(name).compose(this,elide);
	}
	
	Op_rule get_rule(String name) {
		return rule_map.get(name);
	}
	
	int rule_tag(String name) {
		Integer tag=rule_idx.get(name);
		if (tag==null) fault("Undefined rule: "+name);
		return tag;
	}
	
	Op_rule span_rule(Span span) {
		return rules[span.tag];
	}
	
	public String toString() {
		StringBuilder sb=new StringBuilder();
		for (Op_rule rule : rules) sb.append(rule.toString());
		return sb.toString();
	}
}


class Op_rule extends Op { // rule container....
	
	Op_rule(String name,String defn,Op body) {
		this.name=name; // name defn body
		this.defn=defn; // = or :
		if (body instanceof Op_list) { mode=1; this.body=((Op_list)body).op; }
		else if (body instanceof Op_map) { mode=2; this.body=((Op_map)body).op; }
		else this.body=body; // grammar expression Op
	}
	
	String name, defn;
	int mode=0; // default=0 []=1 {}=2
	Op body;
	boolean composed=false;
		
	void compose(Op_rules rules) {
		if (composed) return;
		composed=true;
		body=body.compose(rules,defn.equals(":"));
	}

	public String toString() {
		return String.format("%s %s %s%n",name,defn,body);
	}
}

class Op_map extends Op { // { body.. } 

	Op_map(Op op) {
		this.op=op;
	}

	Op op;
}

class Op_list extends Op { // [ body.. ] 

	Op_list(Op op) {
		this.op=op;
	}

	Op op;
}


class Op_call extends Op { // reference to a rule name.....
	
	Op_call(String name, boolean elide) {
		this.name=name;   // rule name x
		this.elide=elide; // true for `x
	}
	
	Op_call(String name) {
		this.name=name;
		this.elide=false;
	}
	
	String name;
	Op_rules rules;
	Op_rule rule;
	int tag; // name id
	Op body; // Op code rules to evaluate grammar expression
	boolean elide; // no syntax tree node, true for `x or x in a terminal rule
	
	Op compose(Op_rules rules, boolean elide) {
		this.rules=rules;
		if (elide) this.elide=elide;
		tag=rules.rule_tag(name);
		rule=rules.get_rule(name);
		if (!rule.composed) rule.compose(rules);
		body=rule.body;
		if (elide && body.isChs()) return body;
		return this;
	}

	Pen parse(Pen p) {
		Pen p1 = body.parse(p);
		if (p1==null || elide) return p1;
		Span tree=new Span(tag,p.pos,p1.pos,p.tree,p1.tree);
		return new Pen(p1.nib,tree,p1.pos);
	}

	public String toString() {
		if (elide) return String.format("run(%s)",name);
		return String.format("call(%s)",name);
	}
}

class Op_seq extends Op { // x y z ...

	Op_seq(Op[] ops) {
		this.ops=ops;
	}

	Op_seq(Object[] ops) {
		// this.ops=(Op[])ops; // if only this could work !! ....
		this.ops=new Op[ops.length];
		for (int i=0;i<ops.length; i++) this.ops[i]=(Op)ops[i];
	}
		
	Op[] ops;

	Op compose(Op_rules rules, boolean elide) {
		List<Op> oplist=new ArrayList<Op>();
		for (Op op : ops) {
			Op op1=op.compose(rules, elide), opx;
			int i=oplist.size();
			if (i>0 && (opx=oplist.get(i-1)).isNotChs()) { // [... !opx op1 ...]
			 	if (op1.isChs()) // !opx op1
					oplist.set(i-1,((Op_chs)op1).excludeChs(opx.getNotChs()));
			 	else if (op1.isNotChs()) { // !opx !op1
					Op_chs chs=opx.getNotChs().mergeChs(op1.getNotChs());
					oplist.set(i-1,new Op_not(chs));
				} else oplist.add(op1);
			} else oplist.add(op1);
		}
		if (oplist.size()==1) return oplist.get(0);
		return new Op_seq(oplist.toArray(new Op[oplist.size()]));
	}

	Pen parse(Pen p) {
		Pen p1=null;
		for (Op op : ops) {
			p1 = op.parse(p);
			if (p1==null) return null;
			p=p1;
		}
		return p1;
	}
	
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("seq(").append(ops[0]);
		for (int i=1;i<ops.length;i++) sb.append(",").append(ops[i]);
		sb.append(")");
		return sb.toString();
	}
}

class Op_sel extends Op {  // x|y|z ...

	Op_sel() {}

	Op_sel(Op[] ops) {
		this.ops=ops;
	}

	Op_sel(Object[] ops) { // Object[] => OP[] cast...
		this.ops=new Op[ops.length];
		for (int i=0;i<ops.length; i++) this.ops[i]=(Op)ops[i];
	}

	Op[] ops;

	Op compose(Op_rules rules, boolean elide) {
		List<Op> oplist=new ArrayList<Op>();
		for (Op op : ops) {
			Op op1=op.compose(rules, elide), opx;
			int i=oplist.size();
			if (i>0 && op1.isChs() && (opx=oplist.get(i-1)).isChs())
				oplist.set(i-1,((Op_chs)op1).mergeChs((Op_chs)opx));
			else oplist.add(op1);
		}
		if (oplist.size()==1) return oplist.get(0);
		return new Op_sel(oplist.toArray(new Op[oplist.size()]));
	}

	Pen parse(Pen p) {
		int best=-1;
		Pen pick=null;
		for (Op op : ops) {
			Pen p1 = op.parse(p);
			if (p1!=null && p1.pos>best) {
				best=p1.pos;
				pick=p1;
			}
		}
		return pick;
	}

	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("sel(").append(ops[0]);
		for (int i=1;i<ops.length;i++) sb.append("|").append(ops[i]);
		sb.append(")");
		return sb.toString();
	}
}

class Op_alt extends Op {  // x/y/z ...

	Op_alt(Op[] ops) {
		this.ops=ops;
	}
	
	Op_alt(Object[] ops) { // Object[] => OP[] cast...
		this.ops=new Op[ops.length];
		for (int i=0;i<ops.length; i++) this.ops[i]=(Op)ops[i];
	}

	Op[] ops;

	Op compose(Op_rules rules, boolean elide) {
		List<Op> oplist=new ArrayList<Op>();
		for (Op op : ops) {
			Op op1=op.compose(rules, elide), opx;
			int i=oplist.size();
			if (i>0 && op1.isChs() && (opx=oplist.get(i-1)).isChs())
				oplist.set(i-1,((Op_chs)op1).mergeChs((Op_chs)opx));
			else oplist.add(op1);
		}
		if (oplist.size()==1) return oplist.get(0);
		return new Op_alt(oplist.toArray(new Op[oplist.size()]));
	}

	Pen parse(Pen p) {
		for (Op op : ops) {
			Pen p1 = op.parse(p);
			if (p1!=null) return p1;
		}
		return null;
	}

	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("alt(").append(ops[0]);
		for (int i=1;i<ops.length;i++) sb.append("/").append(ops[i]);
		sb.append(")");
		return sb.toString();
	}
}

class Op_rep extends Op { // x*
	
	Op_rep(Op op) {
		this.op=op;
	}

	Op op;

	Op compose(Op_rules rules, boolean elide) {
		op=op.compose(rules, elide);
		return this;
	}

	Pen parse(Pen p) {
		Pen x=p;
		while (true) {
			Pen x1 = op.parse(x);
			if (x1==null || x1.pos==x.pos) return x;
			x=x1;
		}
	}

	public String toString() {
		return String.format("%s*",op);
	}
}

class Op_rep1 extends Op { // x+

	Op_rep1(Op op) {
		this.op=op;
		this.rep=new Op_rep(op);
	}

	Op op, rep;

	Op compose(Op_rules rules, boolean elide) {
		op=op.compose(rules, elide);
		rep=rep.compose(rules, elide);
		return this;
	}

	Pen parse(Pen p) {
		Pen p1 = op.parse(p);
		if (p1==null) return null;
		return rep.parse(p1);
	}

	public String toString() {
		return String.format("%s+",op);
	}
}

class Op_opt extends Op { // x?

	Op_opt(Op op) {
		this.op=op;
	}

	Op op;

	Op compose(Op_rules rules, boolean elide) {
		op=op.compose(rules, elide);
		return this;
	}

	Pen parse(Pen p) {
		Pen p1 = op.parse(p);
		if (p1==null) return p;
		return p1;
	}

	public String toString() {
		return String.format("%s?",op);
	}
}

class Op_not extends Op { // !x

	Op_not(Op op) {
		this.op=op;
	}

	Op op;

	boolean isNotChs() {
		return op.isChs();
	}
	
	Op_chs getNotChs() {
		return ((Op_chs)op);
	}

	Op compose(Op_rules rules, boolean elide) {
		op=op.compose(rules, true);
		return this;
	}

	Pen parse(Pen p) {
		Pen p1 = op.parse(p);
		if (p1==null) return p;
		return null;
	}

	public String toString() {
		return String.format("!%s",op);
	}
}

class Op_isa extends Op { // &x

	Op_isa(Op op) {
		this.op=op;
	}

	Op op;

	Op compose(Op_rules rules, boolean elide) {
		op=op.compose(rules, true);
		return this;
	}

	Pen parse(Pen p) {
		Pen p1 = op.parse(p);
		if (p1==null) return null;
		return p;
	}

	public String toString() {
		return String.format("&%s",op);
	}
}

class Op_pre extends Op { // @name(.name)*
	
	Op_pre() {}

	Op_pre(Object[] xs) {
		names=new String[xs.length];
		for (int i=0;i<xs.length;i++) names[i]=(String)xs[i];
	}

	String[] names;
	int[] tags;

	Op compose(Op_rules rules, boolean elide) {
		tags=new int[names.length];
		for (int i=0;i<names.length;i++) tags[i]=rules.rule_tag(names[i]);
		return this;
	}

	Pen parse(Pen p) {
		Span x=prior(p.tree,tags);
		if (x==null) return null;
		return p;
	}
	
	Span prior(Span x, int[] tags) {
		while (x.top!=null) {
			if (x.tag==tags[0]) {
				for (int i=1;i<tags.length;i++) {
					x=child_tag(x,tags[i]);
					if (x==null) return null;
				}
				return x;
			}
			x=x.tip;
		}
		return null;
	}
	
	Span child_tag(Span x, int tag) {
		Span kid=x.top;
		while (kid!=x.tip) {
			if (kid.tag==tag) return kid;
			kid=kid.tip;
		}
		return null;
	}

	public String toString() {
		String label="";
		for (String name:names) label+=name;
		return String.format("@%s",label);
	}
}

class Op_peq extends Op_pre { // @=name(.name)*

	Op_peq(Object[] xs) {
		names=new String[xs.length-1];
		for (int i=1;i<xs.length;i++) names[i-1]=(String)xs[i];
	}

	Pen parse(Pen p) {
		Span x=prior(p.tree,tags);
		if (x==null) return null;
		// match prior to input...
		String matched=p.nib.input.substring(x.sot,x.eot);
		if (p.nib.input.startsWith(matched,p.pos))
			return p.advanceTo(p.pos+matched.length());
		return null;
	}

	public String toString() {
		String label="";
		for (String name:names) label+=name;
		return String.format("@=%s",label);
	}
}


class Op_empty extends Op { // ''

	Op compose(Op_rules rules, boolean elide) {
		return this;
	}

	Pen parse(Pen p) {
		return p;
	}

	public String toString() {
		return "''";
	}
}

class Op_chs extends Op { // 'x' 42  'x'..'z' 48..57  etc..
	
	int[] ranges;

	Op_chs(int[] ints) {
		if (ints.length==1) ranges=new int[] {ints[0],ints[0]};
		else ranges=ints;
	}

	Op_chs(int i) {
		ranges=new int[] {i,i};
	}

	Op_chs(String[] strs) {
		ranges=new int[strs.length*2];
		for (int i=0;i<strs.length;i++) {
			int k=strs[i].codePointAt(0); // "x"
			ranges[i*2]=k;
			ranges[i*2+1]=k;
		}
	}

	boolean isChs() {
		return true;
	}

	Op compose(Op_rules rules, boolean elide) {
		if (!verify(ranges)) fault(String.format("bad ranges: %s",this));
		return this;
	}

	Pen parse(Pen p) {
		int ch=p.chr;
		if (ch<ranges[0] || ch>ranges[ranges.length-1]) return null;
		int bot=0;
		int top=ranges.length;
		while (bot<top) {
			int pick=bot+((top-bot)>>2<<1); // middle
			if (ch<ranges[pick])
				top=pick; // below min, chop ranges here and above
			else if (ch>ranges[pick+1])
				bot=pick+2; // above max, chop here and below
			else return p.advance();
		}
		return null;
	}

	Op_chs mergeChs(Op_chs other) { // x|y
		int[] xs=(int[])ranges.clone();
		int[] ys=(int[])other.ranges.clone();
		int[] zs=new int[xs.length+ys.length]; // max merge
		int i=0,j=0,k=0; // index for xs,ys,zs
		while (i<xs.length && j<ys.length) {
			int a=xs[i], b=xs[i+1];
			int c=ys[j], d=ys[j+1];
			if (b+1<c) {zs[k++]=a; zs[k++]=b; i+=2;} // a..b  c..d
			else if (d+1<a) {zs[k++]=c; zs[k++]=d; j+=2;} // c..d  a..b
			else if (b>=d) {xs[i]=min(a,c); j+=2;} // overlap a..b higher
			else if (d>=b) {ys[j]=min(a,c); i+=2;} // overlap c..d higher
			else throw new IllegalArgumentException("what?");
		}
		k=absorb(i,xs,zs,k);
		k=absorb(j,ys,zs,k);
		int[] result=new int[k];
		System.arraycopy(zs,0,result,0,k);
		return new Op_chs(result);
	}

	int absorb(int i, int[] xs, int[] zs, int k) {
		if (i>=xs.length) return k; // all done
		if (k==0) {zs[k++]=xs[i]; zs[k++]=xs[i+1]; absorb(i+2,xs,zs,k);}
		int p=zs[k-1], q=xs[i];
		if (p+1>=q) { // overlap....
			zs[k-2]=min(xs[i],zs[k-2]);
			zs[k-1]=max(xs[i+1],zs[k-1]);
			return absorb(i+2,xs,zs,k);
		} 
		while (i<xs.length) zs[k++]=xs[i++];
		return k;
	}

	Op_chs excludeChs(Op_chs other) { // x^y
		int[] xs=(int[])ranges.clone();
		int[] ys=(int[])other.ranges.clone();
		int[] zs=new int[xs.length+ys.length]; // max merge
		int i=0,j=0,k=0; // index for xs,ys,zs
		while (i<xs.length) {
			int a=xs[i], b=xs[i+1];
			if (j<ys.length) {
				int c=ys[j], d=ys[j+1];
				if (b<c) j+=2; // x:a..b < c..d
				else if (d<a) j+=2; // c..d < x:a..b
				else if (c<=a && d>=b) {i+=2; j=0;} // !c..d excludes a..b
				else if (c<=a) {xs[i]=d+1; j+=2;} // d+1..b
				else if (d>=b) {xs[i+1]=c-1; j+=2;} // a..c-1
				else {zs[k++]=a; zs[k++]=c-1; xs[i]=d+1; j+=2;} // split....
			} else { zs[k++]=a; zs[k++]=b; i+=2; j=0; } // ok all excluded, move on...
		}
		int[] result=new int[k];
		System.arraycopy(zs,0,result,0,k);
		return new Op_chs(result);
	}	

	int min(int a, int b) {if (a<b) return a; else return b;}
	int max(int a, int b) {if (a>b) return a; else return b;}
		
	boolean verify(int[] ints) {
		if (ints.length%2!=0) {System.out.printf("chs !verify %d %s",ints[0],ints.length); return false;}
		for (int i=0; i<ints.length; i+=2) if (ints[i]>ints[i+1]) return false;
		return true;
	}

	public String toString() {
		StringBuilder sb=new StringBuilder();
		for (int i=0; i<ranges.length; i+=2) {
			if (i>0) sb.append(",");
			if (ranges[i]==ranges[i+1]) sb.append(String.format("%d",ranges[i]));
			else sb.append(String.format("%d..%d",ranges[i],ranges[i+1]));
		}
		return String.format("chs(%s)",sb.toString());
	}

} //chs

