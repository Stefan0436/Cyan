import java.io.IOException;

public class aac {
	public static class a1 {
		public static boolean test() {
			if (!testAnother().equals("hi")) return false;
			return a1.class.getSimpleName().equals("test");
		}
	}
	
	private static String testAnother() {
		return "hi";
	}

	public static boolean a(String cl1, String cl2) throws IOException {
		if (!a1.test()) return false;
		if (!b(cl1,cl1)) return false;
		if (!new aac().b(10,"10")) return false;
		if (!aaa.b().equals("Cyan")) return false;
		if (!aaa.b("Forge").equals("CyanForge")) return false;
		if (!aab.b("Cyan1").c("Cyan1")) return false;
		if (!aab.b(new String[] { "java.lang.String[]", "java.util.HashMap" }).equals("[Ljava/lang/String;Ljava/util/HashMap;")) return false;
		
		String t1 = aaa.a();
		String t2 = aab.b();
		
		SomeTest.main();
		
		try {
			test3();
			return false;
		} catch (IOException e) {			
		}
		
		return t1.equals(cl1) && t2.equals(cl2);
	}
	public static void test3() throws IOException {
		throw new IOException("Here");
	}
	
	@TestAnno3
	public static boolean b(String test1, String test2) {
		return test1.equals(test2);
	}
	
	@TestAnno2
	private boolean b(int test1, String test2) {
		return (test1+"").equals(test2);
	}
}
