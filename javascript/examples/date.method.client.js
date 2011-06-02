
// HTML page with head containing:
// <script src='../spinachtree.gist.js'></script>
// and in the body src= <this-file> :
// <script src='date.method.clent.js'></script>

// -- object map result: { date... } -------------------

var date_ymd = (function () {
	
	var grammar = // PBNF rules.....
	"	date  = year '-' month '-' day		\n"+
	"	year  : d d d d				\n"+
	"	month : d d?				\n"+
	"	day   : d d?				\n"+
	"	d     : '0'..'9'			\n";
	
	var methods = {
		date: function (y,m,d) {
			return new Date(y,m-1,d);
		}
		
	};
	
	return spinachtree.gist(grammar, methods);
})();

var test="2001-2-3";
var date = date_ymd.transform(test);

alert(date);

// Sat Feb 03 2001 00:00:00 GMT-0500 (EST)
