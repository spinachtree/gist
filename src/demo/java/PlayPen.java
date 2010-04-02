

import org.spinachtree.gist.*;

public class PlayPen {

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

	static String stringof(String... lines) {
		StringBuffer sb=new StringBuffer();
		for (String line: lines) sb.append(line).append("\n");
		return sb.toString();
	}

	
	public static void main(String[] args) {
		System.out.println("PlayPen.....");
		String grammar=gistGrammar;
		Gist play=new Gist(grammar);		
		for (int k=0;k<10000;k++) play.parse(grammar);
		System.out.println("PlayPen.....Done");
		
	}
		
	
}


	
	
	
