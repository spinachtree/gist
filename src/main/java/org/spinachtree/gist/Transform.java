package org.spinachtree.gist;

import org.spinachtree.gist.*;

import java.lang.reflect.*;
import java.util.*;

class Transform {
	
	Transform(Class cls) {
		init(cls);
	}
	
/*	Transform(Object trans,Term term) {
		init(trans.getClass());
		build(trans,term);
	}*/
	
	Map<String,TransMethod> methodMap = new HashMap<String,TransMethod>();
	
	void init(Class cls) { 
		Method[] methods = cls.getDeclaredMethods();
		for (Method method:methods) {
			String name=method.getName();
			TransMethod entry = methodMap.get(name);
			if (entry==null) methodMap.put(name,new TransMethod(method));
			else entry.add(method);  //"multiple method definitions "
		}
	}


	Object build(Object trans, Term term) {
		String name=term.tag();
		TransMethod methods=methodMap.get(name);
		if (methods==null) return term.text();
		Method method=methodMatch(methods,term);
		Class[] types=method.getParameterTypes();
		int arity=types.length;
		Object[] args=new Object[arity];
		int i=0;
		Term arg=term.child();
		if (arg==null)
			args[i++]=term.text();
		else
			while (arg!=null && i<arity) {
				args[i]=buildArg(trans,types[i],arg);
				i+=1;
				arg=arg.next();
			}
		while (i<arity) args[i++]=null;
		return methods.invoke(method,term,trans,args);
	}
	
	Method methodMatch(TransMethod methods, Term term) {
		if (methods.methodCount==1) return methods.get(methods.maxArity);
		int argCount=0;
		Term arg=term.child();
		while (arg!=null && argCount<methods.maxArity) { argCount++; arg=arg.next(); }
		Method method=methods.get(argCount);
		while (method==null && argCount<methods.maxArity) { method=methods.get(++argCount);}
		return method;
	}
	
	Object buildArg(Object trans,Class cls,Term arg) {
		String name=cls.getSimpleName();
		if (name.equals("ArrayList")) return listArgs(trans,arg);
		if (name.equals("Term")) return arg;
		if (name.equals("String")) return arg.text();
		return build(trans,arg);
	}
/*	Object buildArg(Object trans,String className,Term arg) {
		if (className.equals("java.util.ArrayList"))
			return listArgs(trans,arg);
		if (className.equals("oreg.spinachtree.gist.Term"))
			return arg;
		if (className.equals("Term"))
			return arg;
		return build(trans,arg);
	}
*/
	ArrayList<Object> listArgs(Object trans,Term arg) {
		ArrayList<Object> args=new ArrayList<Object>();
		String tag=arg.tag();
		while (arg!=null) {
			args.add(build(trans,arg));
			arg=arg.next(tag);
		}
		return args;
	}

}

class TransMethod {
	TransMethod(Method method) {
		name=method.getName();
		Class[] types=method.getParameterTypes();
		maxArity=types.length;
		for (int k=0;k<=maxArity;k++) methodList.add(null);
		methodList.set(maxArity,method);
		methodCount=1;
	}
	
	void add(Method method) {
		Class[] types=method.getParameterTypes();
		int arity=types.length;
		for (int k=maxArity+1;k<=arity;k++) methodList.add(null);
		Method m=methodList.get(arity);
		if (m!=null)
			invalid(" Can't match: "+name+", method arity: "+arity);
		else {
			methodCount++;
			methodList.set(arity,method);
		}
		if (maxArity<arity) maxArity=arity;
	}
	
	String name;
	ArrayList<Method> methodList=new ArrayList<Method>(10);
	
	int maxArity;
	int methodCount;

	boolean valid=true;
	String msg="";
	
	Method get(int i) { return methodList.get(i); }

	void invalid(String msg) { valid=false; this.msg+=msg; }
	
	Object invoke(Method method,Term term,Object trans,Object[] args) {
		if (!valid) throw new TransformFault(name+msg);
		try { return method.invoke(trans,args); }
		catch (Exception e) {
			String path=term.tag;
			Term t=term;
			while (t.prior!=null) { t=t.prior; path=t.tag+" "+path; }
			Term fail=new Term("-- "+path+" ",term.text,term.sot,term.eot);
			throw new TransformFault(name+": "+e.getCause()+"\n"+report(args)+"\n"+fail);
		}
	}

	String report(Object[] args) { 
		String rep="";
		rep+=name+"(";
		for (int i=0;i<args.length;i++) {
			Object arg=args[i];
			if (i>0) rep+=",";
			if (arg==null) rep+="null";
			else rep+=arg.getClass().getName()+" "+arg.toString();
		}
		rep+=")";
		return rep;
	}
	
}

class TransformFault extends RuntimeException {
	TransformFault(String s) { super(s); }
}
