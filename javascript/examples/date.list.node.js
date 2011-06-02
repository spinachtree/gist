
var spinachtree = require('../spinachtree.gist.js');

// -- simple list result ------------------------------------

var date_ymd = function () {
	
	var grammar = // PBNF rules.....
	"	date  = year '-' month '-' day		\n"+
	"	year  : d d d d				\n"+
	"	month : d d?				\n"+
	"	day   : d d?				\n"+
	"	d     : '0'..'9'			\n";
	
	return spinachtree.gist(grammar);
};
	
var test="2001-2-3";
var date = date_ymd(); // or self execute var defn

console.log( date.transform(test) );

// [ '2001', '2', '3' ]


