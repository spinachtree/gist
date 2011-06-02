
var spinachtree = require('../spinachtree.gist.js');

// -- object map result: { date... } -------------------

var date_ymd = spinachtree.gist(
	"	date  = { year '-' month '-' day }	\n"+
	"	year  : d d d d				\n"+
	"	month : d d?				\n"+
	"	day   : d d?				\n"+
	"	d     : '0'..'9'			\n");
	
var test="2001-2-3";
var date = date_ymd.transform(test);

console.log(date);

// { rule: 'date', year: '2001', month: '2', day: '3' }
