
package org.spinachtree.gist;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

class Transform  {

	private Map<String,Method> ruleMethod = new HashMap<String,Method>();
	
	private static final Class[] parameterTypes={Object[].class};
	
	private Parser parser;
	Object listener;
	
	Transform(Object listener, Parser parser) {
		this.listener=listener;
		this.parser=parser;
		Class listenerClass=listener.getClass();
		Op_rules ops=parser.rules;
		for (Op_rule rule:ops.rules) {
			try {
				ruleMethod.put(rule.name,listenerClass.getMethod(rule.name,parameterTypes));
			} catch (NoSuchMethodException nsme) {
				// should we check if ruleMethod required for this rule...?
				//if (rule.defn.equals("="))
				//	System.out.printf("No public method for: %s%n",rule.name);
			}
		}
		if (ruleMethod.size()==0) System.out.println("No public methods found....%n");
	}
	
	Object transform(String text, Span tree) {
		String rule=parser.span_rule(tree);
		Method method=ruleMethod.get(rule);
		
		Span[] spans=children(tree.tip,tree.top,0);
		Object[] args=new Object[spans.length];
		for (int i=0; i<spans.length; i++) args[i]=transform(text,spans[i]);
		
		if (method==null) {
			// defaults....
			if (args.length==0) return text.substring(tree.sot,tree.eot);
			if (args.length==1) return args[0];
			//System.out.printf("No method for : %s ... (%d)%n",rule,args.length);
			//return null;
			StringBuilder sb=new StringBuilder();
			for (Object arg:args) sb.append(arg.toString());
			return sb.toString();
		} else {
			try {
				return method.invoke(listener,(Object)args); // vararg quirk for Object[]
			} catch(InvocationTargetException ex) {
				System.out.printf("Rule method: %s => %s%n",rule,ex.getTargetException());
				return null;
			} catch(Exception ex) {
				System.out.printf("Rule method: %s => %s%n",rule,ex);
				return null;
			}
		}
	}
	
	Span[] children(Span tip, Span top, int n) {
		if (tip==top) return new Span[n];
		Span[] spans=children(tip,top.tip,n+1);
		spans[spans.length-n-1]=top;
		return spans;
	}
	
	public String toString() {
		return listener.getClass().getName();
	}

} // Transform
