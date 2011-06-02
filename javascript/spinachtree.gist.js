
// == Grammar Input Syntax Transformer ====================================================
// 
// The gist module exports spinachtree.gist
//
// The module is stand-alone with no dependencies.
//
// The only public API is: gist(grammar,methods)
// where grammar is a String that defines PBNF rules,
// and methods is an optional Object containing functions
// with names that correspond to the grammar rule names.
//
// gist(grammar)
//   will create a parser without transform methods
//   .transform(input) for rule transform data results
//
// gist(grammar,methods).transform(input)
//   will parse the input text String and then apply 
//   the methods to the substrings matched by the grammar rules.
//
// =======================================================================================

var spinachtree;  // global var module name, also exports spinachtree.gist
if (!spinachtree) {
	spinachtree = {};
}
//  exports.gist=spinachtree.gist;  // for JSCommon Module (eg Node.js), at end...

spinachtree.gist = function (grammar,methods) { // constructor API...

// == Pen  holds the parser state, immutable...  (well apart from Nib.max)

var pen = function pen(nib,tree,pos) {
	var chr=-1;

	function advance() { return pen(nib,tree,pos+1); }

	if (nib.max<pos) { nib.max=pos; } // mutate 
	if (pos<nib.limit) { chr=nib.input.charCodeAt(pos); }
	return { nib:nib, tree:tree, pos:pos, chr:chr, advance: advance };
};

// == Nib should be immutable but max is used for fault tracking, not used by the parser.

var nib = function (str) {
	return { input:str, limit:str.length, max:0 };
};

// == Span  internal parse tree nodes, immutable....

var span = function (tag, sot, eot, tip, top, mode) {
	return { tag:tag, sot:sot, eot:eot, tip:tip, top:top, mode:mode };
};


// == Op the parser op codes ============================================================

var op = {

	rules: function (ruleList) { // grammar rules....
		var i=0, ruleIdx={}, ruleMap={}, rule;
		for (i=0;i<ruleList.length;i+=1) {
			rule=ruleList[i];
			if (ruleIdx[rule.name]) { throw "Duplicate rule: "+rule.name; }
			ruleIdx[rule.name]=i;
			ruleMap[rule.name]=rule;
		}
		var startName=ruleList[0].name;
		var startCall=this.call(startName);
		var rls = {
			type: 'rules',
			start: startCall,
			
			compose: function () { // links references to rules.....
				for (i=0;i<ruleList.length;i++) { ruleList[i].compose(this); }
				startCall=startCall.compose(this,false);
			},
			
			get_rule: function (name) {
				return ruleMap[name];
			},
			
			toString: function () {
				var sb="";
				for (i=0;i<ruleList.length;i++) {
					sb+=ruleList[i].toString()+"\n";
				}
				return sb;
			}
		};
		if (!startName) {
			throw Error("no rules...");
		}
		rls.compose();
		return rls;
	},

	rule: function (rule, defn, body) {
		var term=(defn===":"), mode=0, composed=false;

		if (body.type==='list') { mode=1; body=body.op; }
		if (body.type==='map') { mode=2; body=body.op; }

		return {
			type: 'rule',
			name: rule,
			defn: defn,
			body: body,
			term: term,
			mode: mode,
			composed: composed,

			compose: function (rules) {
				if (composed) { return this; }
				composed=true;
				body=body.compose(rules,term,rule);
				return this;
			},

			toString: function () {
				var s=rule+defn+body.toString();
				return s;
			}
		};
	},
	
	map: function (op) { //  {  op  }
		return {
			type: 'map',
			op: op
		};
	},

	list: function (op) { //  [  op  ]
		return {
			type: 'list',
			op: op
		};
	},

	call: function (name,elide) {
		var rule, body=null; // set in compose...

		function isChs(x) {
			return (x.type==='chs');
		}

		return {
			type: 'call',
			compose: function (rules,terminal,rn) {
				if (terminal) { elide=true; }
				//tag=rules.rule_tag(name);
				rule=rules.get_rule(name);
				if (rule===undefined) {
					throw Error("undefined rule: "+name);
				}
				//if (!rule.getComposed()) { rule.compose(rules); }
				if (!rule.composed) { rule.compose(rules); }
				body=rule.body; //getBody();
				if (elide && isChs(body)) { return body; }
				return this;
			},
			parse: function (p) {
				var tree, p1 = body.parse(p);
				if (p1===null || elide) { return p1; }
				tree=span(name,p.pos,p1.pos,p.tree,p1.tree,rule.mode);
				return pen(p1.nib,tree,p1.pos);
			},
			toString: function() { 
				if (elide) { return "`"+name; }
				return name;
			}
		};
	},
	
	seq: function (args) { // x y z
		var ops=args, oplist=[];

		function isChs(x) {
			return (x.type==='chs');
		}
		function isNotChs(x) {
			if (x.type!=='nota') { return false; }
			return (x.val().type==='chs');
		}

		return {
			type: 'seq',

			compose: function (rules,elide,rn) {
				var i=0;
				oplist=[];
				for (i=0;i<ops.length;i+=1) {
					var op1=ops[i].compose(rules,elide,rn);
					var j=oplist.length;
					if (j>0 && isNotChs(oplist[j-1])) { // [!opx op1 ]
						var opx=oplist[j-1];
						if (isChs(op1)) {// !opx op1
							oplist[j-1]=op1.exclude(opx.val());
						} else if (isNotChs(op1)) { // !opx !op1
							oplist[j-1]=opx.merge(op1.val());
						} else { oplist.push(op1); }
					} else { oplist.push(op1); }
				}
				ops=oplist;
				if (ops.length===1) { return ops[0]; }
				return this;
			},
			
			parse: function (p) {
				var i, p1=null;
				for (i=0;i<ops.length;i++) {
					p1 = ops[i].parse(p);
					if (p1===null) { return null; }
					p=p1;
				}
				return p1;
			},

			toString: function() {
				var i, op=ops[0].toString();
				for (i=1; i<ops.length; i+=1) { op+=","+ops[i].toString(); }
				return "("+op+")";
			}
		};
	},

	sel: function (args) { // x | y | z
		var i=0, ops=args, oplist=[];

		function isChs(x) {
			return (x.type==='chs');
		}
		
		return {
			type: 'sel',
			compose: function (rules,elide,rn) {
				oplist=[];
				for (i=0;i<ops.length;i++) {
					var op1=args[i].compose(rules,elide,rn);
					var j=oplist.length;
					if (j>0 && isChs(op1) && isChs(oplist[j-1])) {
						// [... opx op1 ...]
						oplist[j-1]=op1.merge(oplist[j-1]);
					} else { oplist.push(op1); }
				}
				ops=oplist;
				if (ops.length===1) { return ops[0]; }
				return this;
			},
			
			parse: function (p) {
				var i, best=-1, pick=null, p1=null;
				for (i=0;i<ops.length;i++) {
					p1 = ops[i].parse(p);
					if (p1!==null && p1.pos>best) {
						best=p1.pos;
						pick=p1;
					}
				}
				return pick;
			},

			toString: function() {
				var i, op=ops[0].toString();
				for (i=1; i<ops.length; i+=1) { op+="|"+ops[i].toString(); }
				return "("+op+")";
			}
		};
	},

	alt: function (args) { // x / y / z  ala PEG
		var ops=args, oplist=[];

		function isChs(x) {
			return (x.type==='chs');
		}
		
		return {
			type: 'alt',
			compose: function (rules,elide,rn) {
				var i, j, op1;
				for (i=0;i<ops.length;i++) {
					op1=ops[i].compose(rules,elide,rn);
					j=oplist.length;
					if (j>0 && isChs(op1) && isChs(oplist[j-1])) {
						// [... opx op1 ...]
						oplist[j-1]=op1.merge(oplist[j-1]);
					} else { oplist.push(op1); }
				}
				ops=oplist;
				if (ops.length===1) { return ops[0]; }
				return this;
			},
			
			parse: function (p) {
				var i, p1;
				for (i=0;i<ops.length;i++) {
					p1 = ops[i].parse(p);
					if (p1!==null) { return p1; }
				}
				return null;
			},

			toString: function() {
				var i, op=ops[0].toString();
				for (i=1; i<ops.length; i+=1) { op+="/"+ops[i].toString(); }
				return "("+op+")";
			}
		};
	},

	rep: function (x) {  // x*
		var opx=x;
		return {
			type: 'rep',
			compose: function (rules,elide,rn) {
				opx=opx.compose(rules,elide,rn);
				return this;
			},
			
			parse: function (p) {
				var x=p, x1=null;
				while (true) {
					x1 = opx.parse(x);
					if (x1===null || x1.pos===x.pos) { return x; }
					x=x1;
				}
				return null;
			},

			toString: function() {
				return opx.toString()+"*";
			}
		};
	},
	rep1: function (x) { // x+ => x x*
		var that=this, opx=x, repx=this.rep(x);
		return {
			type: 'rep1',
			compose: function (rules,elide,rn) {
				opx=opx.compose(rules,elide,rn);
				repx=that.rep(opx);
				return this;
			},
			
			parse: function (p) {
				var p1 = opx.parse(p);
				if (p1===null) { return null; }
				return repx.parse(p1);
			},

			toString: function() {
				return opx.toString()+"+";
			}
		};
	},
	opt: function (x) { // x?  ie x |''
		var opx=x;
		return {
			type: 'opt',
			compose: function (rules,elide,rn) {
				opx=opx.compose(rules,elide,rn);
				return this;
			},
			
			parse: function (p) {
				var p1 = opx.parse(p);
				if (p1===null) { return p; }
				return p1;
			},

			toString: function() {
				return opx.toString()+"?";
			}
		};
	},

	nota: function (x) { // !x
		var opx=x;
		return {
			type: 'nota',
			val: function () { return opx; },
			compose: function (rules,elide,rn) {
				//opx=opx.compose(rules,elide,rn);
				var xx=opx.compose(rules,elide,rn);
				opx=xx;
				return this;
			},
			merge: function(other) {
				opx=opx.merge(other); // chs makes new copy
				return this;
			},
			
			parse: function (p) {
				var p1 = opx.parse(p);
				if (p1===null) { return p; }
				return null;
			},

			toString: function() {
				return "!"+opx.toString();
			}
		};
	},

	isa: function (x) { // &x
		var opx=x;
		return {
			type: 'isa',
			compose: function (rules,elide,rn) {
				opx=opx.compose(rules,elide,rn);
				return this;
			},
			
			parse: function (p) {
				var p1 = opx.parse(p);
				if (p1===null) { return null; }
				return p;
			},

			toString: function() {
				return "&"+opx.toString();
			}
		};
	},

	pre: function (args) { // @ eq? name(.name)*
		var names=[], eq=false, i=0;

		function child_tag(span,tag) {
			var kid=span.top;
			while (kid!==span.tip) {
				if (kid.tag===tag) { return kid; }
				kid=kid.tip;
			}
			return null;
		}

		function prior(span) {
			var i;
			while (span.top!==null) {
				if (span.tag===names[0]) {
					for (i=1;i<names.length;i+=1) {
						span=child_tag(span,names[i]);
						if (span===null) { return null; }
					}
					return span;
				}
				span=span.tip;
			}
			return null;
		}
		
		if (args[0]==="=") { eq=true; i+=1; }
		//for (i;i<args.length;i++) { names.push(args[i]); }
		while (i<args.length) { names.push(args[i]); i+=1; }
		
		return {
			type: 'pre',
			compose: function (rules,elide,rn) {
				return this;
			},
			
			parse: function (p) {
				var i, span=prior(p.tree);
				if (span===null) { return null; }
				if (eq===false) { return p; } // @name predicate
				// @=name...  match prior === input...
				var match=p.nib.input.substring(span.sot,span.eot);
				for (i=0;i<match.length;i++) {
					if (p.chr!==match.charCodeAt(i)) { return null; }
					p=p.advance();
				}
				return p;
			},

			toString: function() {
				var i, s=names[0], sigil="@";
				for (i=1;i<names.length;i+=1) { s+="."+names[i]; }
				if (eq) { sigil+="="; }
				return sigil+s;
			}
		};
	},

	empty: function (x) { // ''
		return {
			type: 'empty',
			compose: function (rules,elide,rn) {
				return this;
			},
			
			parse: function (p) {
				return p;
			},

			toString: function() {
				return "''";
			}
		};
	},

	chs: function (args) { // 'x' 12..34 etc.. 
		var that=this, i, rs=[], arg0=args[0];

		function add_range(x) { rs.push(x); rs.push(x); }
		
		function show_range(i) {
			var x=rs[i], y=rs[i+1];
			if (x===y) { return x; }
			return x+".."+y;
		}
		
		function min(a, b) { if (a<b) { return a; } else { return b; } } 
		function max(a, b) { if (a>b) { return a; } else { return b; } } 
		
		function absorb(i, xs, zs, k) {
			var p, q;
			if (i>=xs.length) { return k; } // all done
			if (k===0) { zs[k++]=xs[i]; zs[k++]=xs[i+1]; absorb(i+2,xs,zs,k); }
			p=zs[k-1]; q=xs[i];
			if (p+1>=q) { // overlap....
				zs[k-2]=min(xs[i],zs[k-2]);
				zs[k-1]=max(xs[i+1],zs[k-1]);
				return absorb(i+2,xs,zs,k);
			} 
			while (i<xs.length) { zs[k++]=xs[i++]; }
			return k;
		}

		function merge_ranges(xs, ys) { // x|y
			var zs=[];
			var i=0, j=0, k=0; // index for xs,ys,zs
			var a, b, c, d;
			while (i<xs.length && j<ys.length) {
				a=xs[i]; b=xs[i+1];
				c=ys[j]; d=ys[j+1];
				if (b+1<c) {zs[k++]=a; zs[k++]=b; i+=2;} // a..b  c..d
				else if (d+1<a) {zs[k++]=c; zs[k++]=d; j+=2;} // c..d  a..b
				else if (b>=d) {xs[i]=min(a,c); j+=2;} // overlap a..b higher
				else if (d>=b) {ys[j]=min(a,c); i+=2;} // overlap c..d higher
				else { throw new Error("what?"); }
			}
			k=absorb(i,xs,zs,k);
			k=absorb(j,ys,zs,k);
			return zs;
		}

		function exclude_ranges(xs, ys) { // x^y
			var zs=[], i=0,j=0,k=0; // index for xs,ys,zs
			var a, b, c, d;
			while (i<xs.length) {
				a=xs[i]; b=xs[i+1];
				if (j<ys.length) {
					c=ys[j]; d=ys[j+1];
					if (b<c) { j+=2; } // x:a..b < c..d
					else if (d<a) { j+=2; } // c..d < x:a..b
					else if (c<=a && d>=b) { i+=2; j=0; } // !c..d excludes a..b
					else if (c<=a) { xs[i]=d+1; j+=2; } // d+1..b
					else if (d>=b) { xs[i+1]=c-1; j+=2; } // a..c-1
					else { zs[k++]=a; zs[k++]=c-1; xs[i]=d+1; j+=2; } // split....
				} else { zs[k++]=a; zs[k++]=b; i+=2; j=0; } // ok all excluded, move on...
			}
			return zs;
		}	

		function copy(a) {
			var i, b=[];
			for (i=0;i<a.length;i+=1) { b.push(a[i]); }
			return b;
		}

		function verify(ints) {
			var i;
			if (ints.length%2!==0) {
				throw new Error("chs: odd ranges... "+ints.length);
			}
			for (i=0; i<ints.length; i+=2) {
				if (ints[i]>ints[i+1]) { throw new Error("chs: bad ranges... "); }
			}
		}
		
		function new_chs(ranges) {
			return that.chs(ranges);
		}

		if (args.length===1) {
			if (typeof arg0 === 'string') { // eg "*+?"
				for (i=0;i<arg0.length;i++) { add_range(arg0.charCodeAt(i)); }
			} else { add_range(arg0); } // n => n..n
		} else { // n,m,... numeric ranges....  
			for (i=0;i<args.length;i+=1) { rs.push(args[i]); }
		}
		verify(rs);
		
		return {
			type: 'chs',
			ranges: rs,
			
			compose: function (rules,elide,rn) {
				return this;
			},
			
			merge: function(other) {
				var rs1=copy(rs), rs2=copy(other.ranges);
				return new_chs(merge_ranges(rs1,rs2));
			},
			exclude: function(other) {
				var rs1=copy(rs), rs2=copy(other.ranges);
				return new_chs(exclude_ranges(rs1,rs2));
			},

			parse: function (p) {
				var ch=p.chr;
				var top=rs.length, bot=0, pick=null;
				if (ch<rs[0] || ch>rs[top-1]) { return null; }
				while (bot<top) {
					pick=bot+((top-bot)>>2<<1); // middle
					if (ch<rs[pick]) {
						top=pick; // below min, chop ranges here and above
					} else if (ch>rs[pick+1]) {
						bot=pick+2; // above max, chop here and below
					} else { return p.advance(); }
				}
				return null;
			},
			
			toString: function() {
				var s, k;
				if (rs.length===2) { return show_range(0); }
				s="chs("+show_range(0);
				for (k=2;k<rs.length;k+=2) { s+="|"+show_range(k); }
				return s+")";
			}
		};
	}

}; // op


// == Parser =====================================================================================

var parser = function (rules) {

	function fault(msg,pen) {
		var text=pen.nib.input, limit=pen.nib.limit;
		var pos=pen.nib.max; // pos=fail point
		var i=pos-1, j=pos, k, sol=0, ch, line, cursor="", report;
		while (i>=0) {
			ch=text.charCodeAt(i);
			if (ch===10 || ch===13) {
				if (sol===0) { sol=i+1; }
				if (pos-i>50) { break; }
			}
			i-=1;
		}
		while (j<limit) {
			ch=text.charCodeAt(j);
			if (ch===10 || ch===13) { break; }
			j+=1;
		}
		line=text.substring(i+1,j).replace("\t"," ");
		k=pos-sol;
		while (k>0) { cursor+=" "; k-=1; }
		report=msg+" at "+pos+" of "+limit+" ... "+"\n"+line+"\n"+cursor+"^  ";
		throw new Error(report);
	}

	return {
		parse: function (str) {
			var nib1=nib(str);
			var root=span(-1,0,0,null,null,0);
			var pen1=pen(nib1,root,0);
			var pen2=rules.start.parse(pen1);
			if (pen2===null) { fault("\nParse failed:",pen1); }
			if (pen2.pos<pen1.nib.limit) { fault("\nParse incomplete:",pen2); }
			return pen2.tree;
		},
		
		toString: function () {
			return rules.toString();
		}
	};
}; // parser

// == Transform ===========================================================================

// walk the parse tree and apply the user methods...

var transform = function transform(text,tree,methods) {
		var spans, i=0, args=[], method;
		
		function children(tip,top) {
			if (tip===top) { return []; }
			var spans=children(tip,top.tip);
			spans.push(top);
			return spans;
		}
		
		function mapof(args) {
			var key, keys={}, hit, map={};
			map.rule=tree.tag;
			if (spans.length===0) {
				map.text=text.substring(tree.sot,tree.eot);
				return map;
			}
			for (i=0; i<spans.length; i+=1) {
				key=spans[i].tag;
				hit=keys[key];
				if (!hit) {
					keys[key]=1;
					map[key]=args[i];
				} else if (hit===1) {
					keys[key]=2;
					map[key]=[map[key],args[i]];
				} else {
					map[key].push(args[i]);
				}
			}
			return map;
		}
		
		spans=children(tree.tip,tree.top);
		
		for (i=0; i<spans.length; i+=1) {
			args.push(transform(text,spans[i],methods));
		}
		
		if (methods) { method=methods[tree.tag]; }

		if (method) {
			if (tree.mode===2) { // { ... } => map
				return method.apply(methods,[mapof(args)]);
			}
			if (args.length==0) { // rule : ... terminal rule ...
				return method.apply(methods,[text.substring(tree.sot,tree.eot)]);
			}
			return method.apply(methods,args);
		} else { // default, no transform method.....
			if (tree.mode===1) { // [ ... ] list
				if (args.length===0) { return [text.substring(tree.sot,tree.eot)] }
				return args;
			}
			if (tree.mode===2) {  // { ... } => map
				return mapof(args);
			}
			// default.....
			if (args.length===0) { return text.substring(tree.sot,tree.eot); }
			if (args.length===1) { return args[0]; }
			return args;
		}

}; // transform


// == Boot =============================================================================

// bootstrap grammar and methods to compile the full PBNF grammar
// compiles itslef with hand compiled op code rules........	

var boot = function() {
	
	var bootGrammar =
	"	pbnf	= (rule | `s)*				\n"+
	"	rule	= name `s defn `s sel			\n"+
	"	sel	= seq (`bar seq)*			\n"+
	"	seq	= rep (`step rep)*			\n"+
	"	rep	= elem sufx?				\n"+
	"	elem	= ref | quots | code | group		\n"+
	"	group	= '(' `s sel `s ')'			\n"+
	"	quots	= quo ('.' '.' quo)?			\n"+
	"	code	= int ('.' '.' int)?			\n"+
	"	ref	= elide? name				\n"+
	"	quo	: 39 (32..38|40..126)* 39		\n"+
	"	int	: '0'..'9'+				\n"+
	"	name	: ('A'..'Z'|'a'..'z')+			\n"+
	"	defn	: '=' | ':'				\n"+
	"	sufx	: '*' | '+' | '?'			\n"+
	"	elide	: '`'					\n"+
	"	ba	: s '|' s				\n"+
	"	step	: ' '* (',' s)?				\n"+
	"	s	: (9 | 10 | 13 | 32)*			\n";

	// Semantics: translate PBNF boot rules into grammar Op expressions.......
	
	var methods = {
		pbnf: function (rules) {
			return op.rules(arguments);
		},
	
		rule: function (name, defn, sel) {
			return op.rule(name, defn, sel);
		},

		sel: function (x) {
			if (arguments.length===1) { return x; }
			return op.sel(arguments);
		},

		seq: function (x) {
			if (arguments.length===1) { return x; }
			return op.seq(arguments);
		},
	
		rep: function (elem,sufx) { // elem sufx? 
			if (!sufx) { return elem; }
			if (sufx==="*") { return op.rep(elem); }
			if (sufx==="+") { return op.rep1(elem); }
			if (sufx==="?") { return op.opt(elem); }
			throw new Error("unknown sufx: "+sufx);
		},
	
		ref: function (x,y) { // elide? name
			if (!y) { return op.call(x,false); }
			return  op.call(y,true);
		},
	
		quots: function (q1,q2) { // 'x' | 'x'..'z' => quo ('.' '.' quo)?
			var len=q1.length, cseq, i;
			if (q2) { return op.chs([q1.charCodeAt(1),q2.charCodeAt(1)]); }
			if (len===2) { return op.empty(); } // ''
			if (len===3) { return op.chs([q1.charCodeAt(1)]); } // 'x'
			cseq=[]; // 'xyz'
			for (i=1;i<len-1;i+=1) { cseq[i-1]=op.chs([q1.charCodeAt(i)]); }
			return op.seq(cseq);
		},

		code: function (x1,x2) { // 123 | 123..456
			if (!x2) { return op.chs([Number(x1)]); }
			return op.chs([Number(x1),Number(x2)]);
		}
	};
		
	// shorthand utility methods......

	function rule(name, assign, expr) { return op.rule(name,assign,expr); }
	function call(name) { return op.call(name,false); }
	function run(name) { return op.call(name,true); }
	function seq() { return op.seq(arguments); }
	function sel() { return op.sel(arguments); }
	function rep(x) { return op.rep(x); }
	function rep1(x) { return op.rep1(x); }
	function opt(x) { return op.opt(x); }
	function chs() { return op.chs(arguments); }
	
	// hand compile PBNF boot rules into Op terms...........................................

	var ruleList = [
		rule("pbnf", "=",  rep(sel(call("rule"),run("s")))),
		rule("rule", "=",  seq(call("name"),run("s"),call("defn"),run("s"),call("sel"))),
		rule("sel",  "=",  seq(call("seq"),rep(seq(run("bar"),call("seq"))))),
		rule("seq",  "=",  seq(call("rep"),rep(seq(run("step"),call("rep"))))),
		rule("rep",  "=",  seq(call("elem"),opt(call("sufx")))),
		rule("elem", "=",  sel(call("ref"),call("quots"),call("code"),call("group"))),
		rule("group","=",  seq(chs("("),run("s"),call("sel"),run("s"),chs(")"))),
		rule("quots","=",  seq(call("quo"),opt(seq(chs("."),chs("."),call("quo"))))),
		rule("code", "=",  seq(call("int"),opt(seq(chs("."),chs("."),call("int"))))),
		rule("ref",  "=",  seq(opt(call("elide")),call("name"))),
		rule("quo",  ":",  seq(chs(39),rep(sel(chs(32,38),chs(40,126))),chs(39))),
		rule("int",  ":",  rep1(chs(48,57))),
		rule("name", ":",  rep1(chs(65,90,97,122))),
		rule("defn", ":",  chs(":=")),
		rule("sufx", ":",  chs("*+?")),
		rule("elide",":",  chs("`")),
		rule("bar",  ":",  seq(run("s"),chs("|"),run("s"))),
		rule("step", ":",  seq(rep(chs(32)),opt(seq(chs(","),run("s"))))),
		rule("s",    ":",  rep(chs(9,10,13,13,32,32)))
	];
	var bootRules = op.rules(ruleList);
	
	return {
		grammar: bootGrammar,
		rules: bootRules,
		methods: methods,
		parser: parser(bootRules),
		compile: function (grammar) {
			var tree=parser(bootRules).parse(grammar);
			var rules=transform(grammar,tree,methods);
			return parser(rules);
		}
	};
}; // boot


// == PBNF =============================================================================

// The full grammar for the PBNF grammar.....

var pbnf = function() {
	
	// PBNF grammar expressed as a boot grammmar:
	// - rule names using letters only
	// - no !x or &x or @x predicates
	// - no hex char codes 0x20
	// - no {many} or [optional] syntax
	// - no x^y syntax
	// - no comments

	var pbnf_grammar = 
	"pbnf    = (rule | `w)*					\n"+
	"rule    = name `w defn `w (map | list | sel)		\n"+
	"map     = '{' `w sel `w '}'				\n"+
	"list    = '[' `w sel `w ']'				\n"+
	"sel     = alt (`w '|' `w alt)*				\n"+
	"alt     = seq (`w '/' `w seq)*				\n"+
	"seq     = rep (`h (',' `w)? rep)*			\n"+
	"rep     = elem repn? | prime				\n"+
	"elem    = item (`w '^' `w item)*			\n"+
	"item    = ref|quots|code|group				\n"+
	"prime   = nota|isa|pre					\n"+
	"group   = '(' `w sel `w ')'				\n"+
	"nota    = '!' `h rep					\n"+
	"isa     = '&' `h rep					\n"+ 
	"pre     = '@' eq? `h name ('.' name)*			\n"+
	"quots   = quo ('..' quo)?				\n"+ 
	"code    = val ('..' val)?				\n"+
	"val     = int | hx hex					\n"+
	"ref     = elide? name ('.' name)?			\n"+
	"name    : alpha (alnum|'_')*				\n"+
	"defn    : '=' | ':'					\n"+
	"repn    : '+'|'?'|'*'					\n"+
	"int     : digit+					\n"+
	"hx      : '0' ('x'|'X')				\n"+
	"hex     : (digit|'a'..'f'|'A'..'F')+			\n"+
	"quo     : 39 (32..38|40..126)* 39			\n"+
	"alnum   : alpha|digit					\n"+
	"alpha   : 'a'..'z'|'A'..'Z'				\n"+
	"digit   : '0'..'9'					\n"+
	"eq      : '='						\n"+
	"elide   : '`'						\n"+
	"blank   : 9|32						\n"+
	"print   : 9|32..1114111				\n"+
	"space   : 9..13|32					\n"+
	"comment : ('--'|'//'|'#') print*			\n"+
	"h       : blank*					\n"+ 
	"s       : space*					\n"+ 
	"w       : (s comment?)*				\n"; 

	// Semantics: translate PBNF rules into grammar Op expressions.......
	
	var pbnf_methods = {
		pbnf: function (rules) {
			return op.rules(arguments);
		},
	
		rule: function (name, defn, body) {
			return op.rule(name, defn, body);
		},

		map: function (body) { //  { body.. }
			return op.map(body);
		},

		list: function (body) {  //  [ body.. ]
			return op.list(body);
		},

		sel: function (x) { // x|y|z|...
			if (arguments.length===1) { return x; }
			return op.sel(arguments);
		},

		alt: function (x) { // x/y/z/...
			if (arguments.length===1) { return x; }
			return op.alt(arguments);
		},

		seq: function (x) { // x,y,z,...
			if (arguments.length===1) { return x; }
			return op.seq(arguments);
		},
	
		rep: function (elem,sufx) { // elem sufx? 
			if (!sufx) { return elem; }
			if (sufx==="*") { return op.rep(elem); }
			if (sufx==="+") { return op.rep1(elem); }
			if (sufx==="?") { return op.opt(elem); }
			throw new Error("unknown sufx: "+sufx);
		},

		elem: function (x) { // x(^y)* => !y.. x
			var i, args=[];
			if (arguments.length===1) { return x; }
			for (i=1;i<arguments.length;i+=1) { args.push(op.nota(arguments[i])); }
			args.push(x);
			return op.seq(args);
		},
	
		nota: function (x) { // !x
			return op.nota(x);
		},

		isa: function (x) { // &x
			return op.isa(x);
		},

		pre: function () { // @ eq? name ('.' name)*
			return op.pre(arguments);
		},

		ref: function (x,y) { // elide? name
			if (!y) { return op.call(x,false); }
			return  op.call(y,true);
		},
	
		quots: function (q1,q2) { // 'x' | 'x'..'z' => quo ('.' '.' quo)?
			var i, len, cseq;
			if (q2) { return op.chs([q1.charCodeAt(1),q2.charCodeAt(1)]); }
			len=q1.length;
			if (len===2) { return op.empty(); } // ''
			if (len===3) { return op.chs([q1.charCodeAt(1)]); } // 'x'
			cseq=[]; // 'xyz'
			for (i=1;i<len-1;i+=1) { cseq[i-1]=op.chs([q1.charCodeAt(i)]); }
			return op.seq(cseq);
		},

		code: function (x1,x2) { // 123 | 123..456
			if (x2) { return op.chs([x1,x2]); }
			return op.chs([x1]);
		},
		
		val: function (int, hex) {  // val = int | hx hex
			if (hex) { return parseInt(hex,16); }
			return Number(int);
		}
	};
	
	return {
		grammar: pbnf_grammar,
		methods: pbnf_methods,
		parser: boot().compile(pbnf_grammar),
		compile: function (grammar) {
			var tree=boot().compile(pbnf_grammar).parse(grammar);
			var rules=transform(grammar,tree,pbnf_methods);
			return parser(rules);
		}
	};
	
}; // pbnf

// == Gist ===============================================================================

	var pbnf_compile = pbnf().compile;

	//var parser=boot().compile(grammar);
	if (!grammar || grammar==='') {
		throw Error("no rules...");
	}
	var grammar_parser=pbnf_compile(grammar);

	return {
		grammar: grammar,

		transform: function (text) {
			var tree=grammar_parser.parse(text);
			return transform(text,tree,methods);
		},
		
		parser: parser // for debug code inspection
	};
	
} // spinachtree.gist

var exports;
if (exports) {
	exports.gist=spinachtree.gist;
}


