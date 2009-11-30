
public class Circular //LazyModules 
{
  static int i = 0;

  static abstract class A{
    abstract B getB();
    int k = i++;
    public String toString() { return "" + k; }
  }

  static abstract class B{
    abstract A getA();
    int k = i++;
    public String toString() { return "" + k; }
  }

  public static void doStuff(A foo, B bar){
    System.out.println("foo = " + foo);
    System.out.println("foo.b = " + foo.getB());
    System.out.println("bar = " + bar);
    System.out.println("bar.a = " + bar.getA()); 
  }

  public static void main(String[] args){
    new Object(){
      final A foo = new A() { B getB() { return bar; }  };
      final B bar = new B() { A getA() { return foo; }  };

      { doStuff(foo, bar); }
    };
  }
}