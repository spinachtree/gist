
package org.spinachtree.gist;

/**
 * Gist is the only public class.
 */
public class Gist {
	
	static Parser gist_parser=null;
	static Transform gist_transform=null;
	
	/**
	 * Construct a parser from the grammar..
	 * @param grammar PBNF grammar rules
	 */
	public Gist(String grammar) {
		initialize(grammar,null);
	}
	
	/**
	 * Construct a parser from the grammar and link into the listener methods..
	 * @param grammar PBNF grammar rules
	 * @param listener semantic methods for grammar rules (they must be public)
	 */
	public Gist(String grammar, Object listener) {
		initialize(grammar,listener);
	}
	
	private void initialize(String grammar, Object listener) {
		this.grammar=grammar;
		if (gist_parser==null) bootstrap();
		parser=new Parser(compile(grammar));
		transform=new Transform(listener,parser);
	}
	
	String grammar;
	Parser parser;
	Transform transform;
	
	/**
	 * parse the input text and run any corresponding listener methods..
	 * <p>A rule result will be a String for a terminal rule, or
	 * an Object for a one component rule, or an Object[] for
	 * two or more components, or a Map&lt;String,Object&gt; for
	 * rule body map notation: {...}, or any Object generated by
	 * a transform method.
	 * <p>The argument list for a transform method is always an Object[],
	 * and the method may return any desired result type.
	 * @param text input string
	 * @return rule data result or the result of listener method
	 */
	public Object transform(String text) {
		Span span=parser.parse(text);
		return transform.transform(text,span);
	}

	private Object[] compile(String grammar) {
		Span span=gist_parser.parse(grammar);
		return ((Object[])gist_transform.transform(grammar,span));
	}
	
	private void bootstrap() {
		Boot boot=new Boot();
		Op_rule[] rules=boot.rules;
		gist_parser=new Parser(rules);
		gist_transform=new Transform(boot,gist_parser);
		// replace boot gist_parser with pbnf gist_parser....
		PBNF pbnf=new PBNF();
		Span span=gist_parser.parse(pbnf.pbnfGrammar);
		Object[] rls=(Object[])gist_transform.transform(pbnf.pbnfGrammar,span);
		gist_parser=new Parser(rls);
		gist_transform=new Transform(pbnf,gist_parser);
	}
	
	private Op op_call(String name, boolean elide) {
		return parser.op_call(name,elide);
	}
	
	/**
	 * internal parser rule code for debug inspection..
	 */
	public String ruleCode() {
		return parser.toString();
	}
	
	/**
	 * the PBNF grammar rules..
	 */
	public String toString() {
		return transform.toString()+"\n"+grammar;
	}

} // Gist



