
package org.spinachtree.gist;

import java.util.*;

class Boot {
	
	Boot() {
		ruleMap=new HashMap<String,Rule>();
		for (Rule rule : rules()) { ruleMap.put(rule.name,rule); }
	}

	HashMap<String,Rule> ruleMap;
	
	Term parse(String str) {
		Scan scan=new Scan(str,0);
		boolean result=ruleMap.get("Gist").parse(scan);
		if (!result || scan.pos<scan.eot)
			throw new UnsupportedOperationException("Boot failed... ");
		return scan.root();
	}

	/* -- bootstrap grammar.......................

		Gist	= (Rule / ws)*
		Rule    = name sp defn Body
		Body    = Seq (ws '/' Seq)*
		Seq     = (sp Factor Repeat?)+
		Factor  = Ref / Phrase / Literal / Prior
		Ref     = name
		Phrase  = '(' Body ws ')'
		Prior   = '@' name
		Literal = Quote / Code
		Quote   = quote ('..' quote)? 
		Code	= int ('..' int)?
		Repeat  = '*'/'+'/'?'
		defn    : ':'/'='
		name    : ('a'..'z'/'A'..'Z')+
		int     : '0'..'9'+
		quote   : 39 (32..38/40..126)* 39
		sp      : (' '/9)*
		ws      : (' '/9..13)*
	*/
	
	// hand compiled rule list for the boot grammar...

	Rule[] rules() {
		Rule[] rls={
		rule("Gist",rep(sel(ref("Rule"),ref("ws")))),
		rule("Rule",seq(ref("name"),ref("sp"),ref("defn"),ref("Body"))),
		rule("Body",seq(ref("Seq"),rep(seq(ref("ws"),ch('/'),ref("Seq"))))),
		rule("Seq",rep1(seq(ref("sp"),ref("Factor"),opt(ref("Repeat"))))),
		rule("Factor",sel(ref("Ref"),ref("Literal"),ref("Phrase"),ref("Prior"))),
		rule("Ref",ref("name")),
		rule("Phrase",seq(ch('('),ref("Body"),ref("ws"),ch(')'))),
		rule("Prior",seq(ch('@'),ref("name"))),
		rule("Literal",sel(ref("Quote"),ref("Code"))),
		rule("Quote",seq(ref("quote"),opt(seq(ch('.'),ch('.'),ref("quote"))))),
		rule("Code",seq(ref("int"),opt(seq(ch('.'),ch('.'),ref("int"))))),
		rule("Repeat",sel(ch('*'),ch('+'),ch('?'))),
		term("defn",sel(ch(':'),ch('='))),
		term("name",rep1(ch('A','Z','a','z'))),
		term("quote",seq(cc(39),rep(cc(32,38,40,126)),cc(39))),
		term("int",rep1(ch('0','9'))),
		term("sp",rep(cc(9,9,32,32))),
		term("ws",rep(cc(9,13,32,32)))};
		return rls;
	}
	
	Rule rule(String name, ParseOp body) { return new Rule(name,body,false); }

	Rule term(String name, ParseOp body) { return new Rule(name,body,true); }

	ParseOp ref(String name) { return new Ref(name,false,null,ruleMap,null); }

	ParseOp seq(ParseOp... args) { return new Seq(args); }

	ParseOp sel(ParseOp... args) { return new Select(args); }

	ParseOp rep(ParseOp arg) { return new Repeat(arg,0); }

	ParseOp rep1(ParseOp arg) { return new Repeat(arg,1); }

	ParseOp opt(ParseOp arg) { return new Repeat(arg,0,1); }
		
	ParseOp ch(char x) { return ch(x,x); }
	ParseOp ch(char... chs) { // char[]=>int[], must be a better way...
		int[] ranges=new int[chs.length];
		for (int i=0;i<chs.length;i++) { ranges[i]=(int)chs[i]; }
		return new Chars(ranges);
	}
	ParseOp cc(int i) { return cc(i,i); }
	ParseOp cc(int... ranges) { return new Chars(ranges); }

} // Boot
