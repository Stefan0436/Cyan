import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.asf.cyan.TestAnno;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
public class aaa extends CyanComponent implements Iterable<String>, Closeable {
	@TestAnno3
	public String a = "hi";	
	public boolean test = false;

	public static String a()throws IOException {
		aaa a2 = new aaa();
		System.out.println(a2.getClass().getTypeName());
		a2.a = "test";
		a2.c(a2.a);
		try {
			a2.close();
		} catch (IOException e) {
		}
		return aaa.class.getSimpleName();
	}

	static String b() {
		return "Cyan";
	}

	public static String b(String a) {
		return "Cyan" + a;
	}
	
	public boolean c(@TestAnno3 String a) throws IOException {
		return c(a, "");
	}

	@TestAnno(test1 = 1, test2 = "hi", test3 = 4.1d, test4 = InjectLocation.HEAD)
	@TestAnno2
	public boolean c(@TestAnno3 String a, String b) throws IOException {
		if (!aac.a1.test()) return false;
		return a.equals(this.a);
	}

	@Override
	public Iterator<String> iterator() {
		return null;
	}

	@Override
	public void close() throws IOException {
		
	}
}
