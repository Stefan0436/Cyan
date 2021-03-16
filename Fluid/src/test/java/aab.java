import org.asf.cyan.fluid.Fluid;

public class aab {
	public static aaa b(String test) {
		aaa t = new aaa();
		t.a = test;
		return t;
	}
	public static String b() {
		return aab.class.getSimpleName();
	}
	public static String b(String[] types) {
		return Fluid.getDescriptors(types);
	}
}
