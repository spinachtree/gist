
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
		if (listener==null) return;
		// find listener transform methods.........
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
		Op_rule rule=parser.span_rule(tree);
		String ruleName=rule.name;
		Method method=ruleMethod.get(rule.name);
		
		Span[] spans=children(tree.tip,tree.top,0);
		Object[] args=new Object[spans.length];
		for (int i=0; i<spans.length; i++) args[i]=transform(text,spans[i]);
		
		if (method==null) {
			if (rule.mode==1) { // [ ... ] list
				if (args.length!=0) return args;
				return new Object[] { text.substring(tree.sot,tree.eot) };
			}
			if (rule.mode==2) {  // { ... } => map
				return mapof(ruleName,text,spans,args);
			}
			// defaults....
			if (args.length==0) return text.substring(tree.sot,tree.eot);
			if (args.length==1) return args[0];
			return args;
		} else { // transform method.....
			if (rule.mode==2) { // { ... } => map
				Object[] maparg = new Object[] { mapof(ruleName,text,spans,args) };
				return applyMethod(ruleName,method,maparg);
			}
			if (args.length==0) { // rule : ... terminal rule ...
				Object[] listarg = new Object[] { text.substring(tree.sot,tree.eot) };
				return applyMethod(ruleName,method,listarg);
			}
			return applyMethod(ruleName,method,args);
		}
	} // transform

	Object applyMethod(String rule, Method method, Object[] args) {
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

	Map<String,Object> mapof(String rule, String text, Span[] spans, Object[] args) {
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("rule",rule);
		if (spans.length==0) {
			Span span = spans[0];
			map.put("text",text.substring(span.sot,span.eot));
			return map;
		}
		Map<String,Integer> keys = new HashMap<String,Integer>();
		for (int i=0; i<spans.length; i++) {
			String key=parser.span_rule(spans[i]).name;
			Integer hit=keys.get(key);
			if (hit==null) {
				keys.put(key,new Integer(1));
				map.put(key,args[i]);
			} else if (hit.intValue()==1) {
				keys.put(key,new Integer(2));
				List<Object> vals = new ArrayList<Object>();
				vals.set(0,map.get(key));
				vals.set(1,args[i]);
				map.put(key,vals);
			} else {
				List<Object> vals= (List<Object>) map.get(key);
				map.put(key,vals.add(args[i]));
			}
		}
		return map;
	}
	
	Span[] children(Span tip, Span top, int n) {
		if (tip==top) return new Span[n];
		Span[] spans=children(tip,top.tip,n+1);
		spans[spans.length-n-1]=top;
		return spans;
	}
	
	public String toString() {
		if (listener==null) return "No transform methods..";
		return listener.getClass().getName();
	}

} // Transform
