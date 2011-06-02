
var spinachtree = require('../spinachtree.gist.js');

var calc = spinachtree.gist(
"	arith = term (add term)*	\n"+
"	term  = val (mult val)*		\n"+
"	val   = num | '(' arith ')'	\n"+
"	add   : '+' | '-'		\n"+
"	mult  : '*' | '/'		\n"+
"	num   : '0'..'9'+		\n");
	
var test = "1+2*(8-6/2)-3";
var clist = calc.transform(test);

var ans = reduce(Number(clist[0]),1,clist);

function reduce(x,i,lst) {
	var y, z;
	if (i>=lst.length) return x;
	y=lst[i+1];
	if (typeof y === 'string') {
		return reduce(arith(x,lst[i],Number(y)),i+2,lst);
	} else {
		z=reduce(Number(y[0]),1,y);
		return reduce(arith(x,lst[i],z),i+2,lst);
	}
}

function arith(x,op,y) {
	if (op==='+') return x+y;
	if (op==='-') return x-y;
	if (op==='*') return x*y;
	if (op==='/') return x/y;	
}

console.log( ans ); // 8

