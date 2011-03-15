
package org.spinachtree.gist;

class Span {

	Span(int tag, int sot, int eot, Span tip, Span top) {
		this.tag=tag; // rule name id
		this.sot=sot; // start of text
		this.eot=eot; // end of text
		this.tip=tip; // prior span 
		this.top=top; // last child
	}

	int tag, sot, eot;
	Span tip, top;

}
