
package org.spinachtree.gist;

class Pen {

	Pen(Nib nib, Span tree, int pos) {
		this.nib=nib; // input
		this.tree=tree; // span tree
		this.pos=pos;   // cursor index
		if (nib.max<pos) nib.max=pos;
		if (pos>=nib.limit) chr=-1;
		else chr=nib.input.codePointAt(pos);
	}

	Nib nib;
	Span tree;
	int pos, chr;
	
	Pen advance() {
		return new Pen(nib,tree,pos+1);
	}

	Pen advanceTo(int k) {
		return new Pen(nib,tree,k);
	}

}

class Nib {
	
	Nib(String str) {
		input=str;
		limit=str.length();
		max=0;
	}
	
	String input;
	int limit,max;
	
}
