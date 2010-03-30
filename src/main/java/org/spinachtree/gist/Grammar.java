package org.spinachtree.gist;

import java.util.*;

class Grammar {

	static Rules rules(Term tree) { return (new Grammar(tree).rules); }
		
	static Transform transform; // to walk parse tree and call Java methods
	
	Grammar(Term gistTree) {
		if (transform==null) transform=new Transform(this.getClass());
		compile=new Compile(transform,rules);
		compile.rules(this,gistTree);
	}

	Rules rules=new Rules();
	Compile compile;

	// Full Gist language grammar, but expressed using Boot grammar language...
	// Boot does not accept: comments, !, @, hex 0xff, external refs, etc..

	public static final String gistGrammar = stringof(
		"rules   = (xs (rule/import) s ';'?)* xs ",
		"rule    = name elide? xs defn xs sel ",
		"sel     = seq xs '/' xs sel / seq ", 
		"seq     = item s (',' xs)? seq / item ", 
		"item    = factor rep? / prime ",
		"rep     = '+'/'?'/'*' ints? ",
		"factor  = fact (s butnot s fact)* ",
		"fact    = ident/literal/group/prior/event ", 
		"prime   = option/many/not/peek ", 
		"group   = '(' exp ')' ", 
		"option  = '[' exp ']' ", 
		"many    = '{' exp '}' ",
		"exp     = (xs sel xs)* ",
		"not     = '!' s factor ", 
		"peek    = '&' s factor ", 
		"prior   = '@' s name ", 
		"event   = '<' s name? s args? '>' ", 
		"literal = quote / str / code ", 
		"quote   = quo ('..' quo)? ", 
		"code    = '0' ('x'/'X') hexs / ints ", 
		"ints    = int ('..' (int/anon))? ", 
		"hexs    = hex ('..' ('0'('x'/'X'))? hex)? ", 
		"ident   = ref / anon / skip / newln ",
		"ref     = path name elide? ",
		"import  = '_' s defn s label '._'? (s ','? s label '._'?)* ",
		"label   : name ('.' name)* ",
		"path    : (name '.')*  ",
		"name    : alpha (alnum/'_')* ",
		"defn    : '=' / ':' ", 
		"int     : digit+ ", 
		"hex     : (digit/'a'..'f'/'A'..'F')+ ", 
		"quo     : 39 (32..38/40..126) 39 ", 
		"str     : 39 (32..38/40..126)* 39 ", 
		"s       : blank* ", 
		"xs      : (sp* comment?)* ", 
		"comment : ('--' / '//' / '#') print* ",
		"args    : (32..61/63..126)+ ", // !'>'
		"alnum   : alpha/digit ", 
		"alpha   : 'a'..'z'/'A'..'Z' ", 
		"digit   : '0'..'9' ", 
		"elide   : '..' ", 
		"colon   : ':' ", 
		"butnot  : '-!' ", 
		"skip    : '~' ", 
		"newln   : '$' ", 
		"anon    : '_' ", 
		"blank   : 9/32 ", 
		"print   : 9/32..1114111 ",
		"sp      : 9..13/32 " );

	// Transform methods for gistGrammar rules ....................................
	
	Op sel(Op x, String xs1, String xs2, Op y) { return x.or(y); }
	Op seq(Op x, String s, Op y) { return x.and(y); }
	Op seq(Op x, String s, String xs, Op y) { return x.and(y); }
	Op item(Op x, String rep) {
		if (rep==null) return x;
		if (rep.equals("*")) return x.rep();
		if (rep.equals("+")) return x.rep1();
		if (rep.equals("?")) return x.opt();
		else return x;
	}
	Op factor(ArrayList<Op> ops) { return notList(1,ops.get(0),ops); }
	Op group(Op x) { if (x.And==null && x.Or==null) return x; else return new Grp(x); }

	Op fact(Op x) { return x; }
	Op prime(Op x) { return x; }
	Op option(Op x) { return group(x).opt(); }
	Op many(Op x) { return group(x).rep(); }
	Op literal(Op x) { return x; }
	Op code(Op x) { return x; }
	Op ident(Op x) { return x; }
	
	Op exp(String _, ArrayList<Op> ops, String _x) { return andList(1,ops.get(0),ops); }

	Op not(String _s, Op x) { return new Not(x); }
	Op peek(String _s, Op x) { return new Peek(x); }
	Op prior(String _s, Op x) { return new Prior(x); }

	Op ref(String path, String name, String elide) { return compile.ruleRef(path,name,elide); }

        Op skip(String _) { return new WhiteSpace(); } // ~ => xs..*
        Op newln(String _) { return new NewLine(); } // $ => XML1.1 eol
        Op anon(String _) { return new Chs(new int[] {0,0x10FFFF},1); } // _ => any

	Op event(String _s, String name,String _s1, String args) { return new Event(compile.host(),name,args); }

	Op ints(String min, String max) { return new Chs(min,max); }
	Op hexs(String min, String max) { return new Chs(min,max,16); }
	Op quote(String q1, String q2) { return new Chs(q1,q2); }
	Op str(String s) { return new Str(s); }

	Op andList(int i, Op x, ArrayList<Op> ops) {
		if (i>ops.size()-1) return x;
		return andList(i+1,x.and(ops.get(i)),ops);
	}

	Op notList(int i, Op x, ArrayList<Op> ops) {
		if (i>ops.size()-1) return x;
		Op y=ops.get(i);
		if (x.except(y)) return notList(i+1,x,ops);
		return new Not(y).and(notList(i+1,x,ops));
	}

	static String stringof(String... lines) {
		StringBuffer sb=new StringBuffer();
		for (String line: lines) sb.append(line).append("\n");
		return sb.toString();
	}
	
	public String toString() {
		return rules.toString();
	}

} // Grammar


