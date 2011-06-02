
var spinachtree = require('../spinachtree.gist.js');

// JSON -- see: http://www.json.org/

var json = (function () {
	
	var grammar = // PBNF rules.....
	"	json   = `s (obj | array) `s                     \n"+
	"	obj    = '{' `s pairs? `s '}'                    \n"+
	"	pairs  = pair (`s ',' `s pair)*                  \n"+
	"	pair   = string `s ':' `s val                    \n"+
	"	array  = '[' `s vals? `s ']'                     \n"+
	"	vals   = val (`s ',' `s val)*                    \n"+
	"	val    = obj|array|string|number                 \n"+
	"	        |true_|false_|null_                      \n"+
	"	string = `quot str `quot                         \n"+
	"	number : int frac?                               \n"+
	"	int    : neg? digits                             \n"+
	"	frac   : '.' digit+ exp?                         \n"+
	"	exp    : ('e'|'E') sign? digit+                  \n"+
	"	digits : '0' | '1'..'9' digit*                   \n"+
	"	digit  : '0'..'9'                                \n"+
	"	sign   : '+'|'-'                                 \n"+
	"	neg    :  '-'                                    \n"+
	"	true_  : 'true'                                  \n"+
	"	false_ : 'false'                                 \n"+
	"	null_  : 'null'                                  \n"+
	"	hex    : digit|'a'..'f'|'A'..'F'                 \n"+
	"	str    : (char|esc)*                             \n"+
	"	esc    : bs (code | 'u' hex hex hex hex)         \n"+
	"	code   : bs|fs|quot|'b'|'f'|'n'|'r'|'t'          \n"+
	"	bs     : 92   -- back-slash                      \n"+
	"	fs     : 47   -- forward-slash                   \n"+
	"	quot   : 34   -- quote                           \n"+
	"	s      : (9|10|13|32)*                           \n"+
	"	char   : 0x20..0x10ffff^quot^bs                  \n";
	
	// --- transform method for json grammar rules -----------------------------

	var methods = {

		json: function (x) {
			return x;
		},

		obj: function (pairs) {
			var map = {}, pair, i;
			for (i=0;i<pairs.length;i+=1) {
				pair = pairs[i];
				map[pair[0]] = pair[1];
			}
			return map;
		},

		pairs: function () {
			return arguments;
		},

		array: function (vals) {
			var arr=[], i;
			for (i=0;i<vals.length;i+=1) {
				arr.push(vals[i]);
			}
			return arr;
		},

		vals: function () {
			return arguments;
		},

		string: function (s) {
			return String(s);
		},

		number: function (n) {
			return Number(n);
		},

		true_: function () {
			return true;
		},

		false_: function () {
			return false;
		},

		null_: function () {
			return null;
		}

	};
	
	return spinachtree.gist(grammar, methods);
})();

// -- test ---------------------------------------

var test = " { \"a\" : true, \"b\" : false,"+
	   "\"list\" : [123.45, \"one\\ttwo\", null] }";

var data = json.transform(test);

console.log(data);

