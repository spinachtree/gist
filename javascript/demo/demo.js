
// Gist Demo....

// requires: spinachtree.gist
// load with: <script src='../spinachtree.gist.js'></script>

//console.log('demo...');

var grammars = {
	date0: ["date  = year '-' month '-' day",
		"year  : d d d d",
		"month : d d?",
		"day   : d d?",
		"d     : '0'..'9'"],
	date1: ["date  = year '-' month '-' day ",
		"year  = d d d d",
		"month = d d?",
		"day   = d d?",
		"d     = '0'..'9'"],
	date2: ["date  = year sep month sep day ",
		"sep   : ('-'|'/'|' '+)  ",
		"year  : d d d d",
		"month : d d?",
		"day   : d d?",
		"d     : '0'..'9'"],
	date3: ["date  = year `sep month `sep day ",
		"sep   : ('-'|'/'|' '+)  ",
		"year  : d d d d",
		"month : d d?",
		"day   : d d?",
		"d     : '0'..'9'"],
	date4: ["date  = { year `sep month `sep day } ",
		"sep   : ('-'|'/'|' '+)  ",
		"year  : d d d d",
		"month : d d?",
		"day   : d d?",
		"d     : '0'..'9'"],

	expr: ["arith = term (add term)*",
		"term  = val (mult val)*",
		"val   = num | '(' arith ')'",
		"add   : '+' | '-'",
		"mult  : '*' | '/'",
		"num   : '0'..'9'+" ],
	uri: [  "uri      = { scheme authority path query fragment } ",
		"scheme   : char^(':'|'/'|'?'|'#')+  ':' | '' ",
		"authority: '//' char^('/'|'?'|'#')* | '' ",
		"path     : char^('?'|'#')*",
		"query    : ('?' char^'#'*)? ",
		"fragment : ('#' char*)? ",
		"char     : 33..126" ],
	json: [ "json   = `s (object | list) `s",
		"object = { '{' `s pairs? `s '}' }",
		"list   = '[' `s values? `s ']'",
		"pairs  = `s pair (`comma pair)* ",
		"pair   = name `s ':' `s value ",
		"values = value (`comma value)* ",
		"value  = object | list | string | number | literal",
		"string = `quo (chs|esc)* `quo",
		"esc    = {`bslash ('u' hex4 | char) } ",
		"hex4   : hex hex hex hex ",
		"number : '-'? int ('.' d+)? (('e'|'E') ('+'|'-')? d+)? ",
		"literal: 'true' | 'false' | 'null' ",
		"name   : string ",
		"chs    : char^(quo|bslash)+ ",
		"hex    : '0'..'9'|'A'..'F'|'a'..'f' ",
		"int    : d | '1'..'9' d* ",
		"comma  : s ',' s ",
		"quo    : 34",
		"bslash : 92",
		"d      : '0'..'9' ",
		"s      : space* ",
		"space  : 9|10|13|32 ",
		"graph  : 33..0x10FFFF ",
		"char   : 1..0x10FFFF "],
	xml: [  "xml    = (elem | entity | text)* ",
		"elem   = '<' tag atts? `s ('>' xml '</' tag '>' | '/>') ",
		"entity : '&' graph^';'* ';'",
		"tag    : graph^('>'|'/')+ ",
		"atts   : (char^('>'|'/'|amp|quo) | string)+ ",
		"text   : char^('<'|'&')* ",
		"string : quo char^quo* quo | amp char^amp* amp ",
		"amp    : 39 ",
		"quo    : 34 ",
		"s      : space* ",
		"space  : 9|10|13|32 ",
		"graph  : char^space ",
		"char   : 1..0x10FFFF "],
	blank: [""]
};

var samples = {
	date0: "2010-12-13",
	date1: "2010-12-13",
	date2: "2010-12/13",
	date3: "2010 12/13",
	date4: "2010-12-13",

	expr: "1+2*(8-6/2)-3",
	uri: "http://www.ics.uci.edu/pub/ietf/uri/#Related",
	json: "{\"foo\": \"ab\\u00efcd\", \"bar\": [1,2,3]}",
	xml: "<foo class='hi'>text with <em>emphasis</em> ... </foo>",
	blank: ""
};

var notes = {
//	examples: "Requires JavaScript: Safari 5.x, Firefox 4.x",

	date0: "Simple list of strings matched by date rule.\n\n"+
	"Note that the list items are named rules, \n"+
	"but not the literal '-' matches. \n",
	date1: "The rules are now all defined with '=' so they all\n"+
	"contribute their sub-rule matches to build up nested lists.\n\n"+
	"The previous example used ':' to match terminal strings.\n",

	date2: "The 'sep' rule matches various separators... \n"+
	"\nYou may not need the full result, to match the sep \n"+
	"as a literal (without its result) see the next example... \n",
	date3: "The `sep rule now has a back-tick prefix, \n"+
	"which deletes it from the result...\n",
	date4: "Instead of a list of strings we can translate the \n"+
	"result into an object (ie a key:value map).\n"+
	"Using braces { ... } around a rule body does the trick.\n"+
	"\n"+
	"You could try adding {...} around the month rule too...\n"+
	"\n"+
	"The list/object results follow the Json data model and \n"+
	"they are implemented in the host programming language as\n"+
	"native data structures (in this case in JavaScript).\n",

	expr: "List of matched terms nested for operator precedence.\n"+
	"\nA recursive reduce left function can be used to evaluate these lists.",
	uri: "RFC 3986 gives this regex to match a URI:\n"+
	" (([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?\n\n"+
	"The grammar follows the regex, but it could do better.",
	json: "An application would want the data as itself...\n\n"+
	"Objects and lists can be generated from the rule results,\n"+
	"but rule transform methods make it even easier (see the API).\n",
	xml: "simple but useful ...\n",

	blank: "Cut-Paste your own grammars and input samples..."
};

var grammar=document.getElementById('grammar');
var source=document.getElementById('source');
var note=document.getElementById('note');
var nb1=document.getElementById('nb1');
var nb2=document.getElementById('nb2');

grammar.value=line_list(grammars['date0']);
source.value=samples['date0'];
note.innerHTML=note_lines('date0'); //notes['date0'];

function note_lines(key) {
	return notes[key].replace(/\n/g,"<br />");
}

function line_list(lines) {
	var str="";
	for (var i=0;i<lines.length;i+=1) {
		str+=lines[i]+"\n";
	}
	return str;
}

// simple pretty print of results.....

function print_elem(x) {
	if (typeof x == "string") {
		x=x.replace(/"/g,"\\\"");
		return '"'+x+'"';
	} else if ((Array.isArray && Array.isArray(x)) || x.length) {
		return print_array(x);  // isArray ECMA 5
	} else if (x.rule) { // should be a better test...
		return print_obj(x);
	} else {
		return x.toString();
	}
}

function print_array(arr) {
	var str="[";
	for (var i=0;i<arr.length;i+=1) {
		var x=arr[i];
		if (i>0) str+=", ";
		str+=print_elem(x);
	}	
	return str+"]";	
}

function print_obj(obj) {
	var i=0, str="{";
	for (var key in obj) {
		if (i>0) str+=", ";
		i+=1;
		str+=key+': ';
		str+=print_elem(obj[key]);
	}	
	return str+"}";	
}

// select menu example....

function do_example(form, value) {
	form.grammar.value = line_list(grammars[value]);
	form.source.value = samples[value];
	note.innerHTML = note_lines(value); //notes[value];
	form.result.value = "";
}

// hit GO....

function do_transform(form) {
	var src=form.source.value;
	var demo, result;
	try {
		demo=spinachtree.gist(form.grammar.value);
		result=print_elem(demo.transform(src));
	} catch(e) {
		result=e.toString();
		transform=null;
	}
	form.result.value = result;
}

