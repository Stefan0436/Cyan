package org.asf.cyan.api.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CyanComponentTest extends CyanComponent {

	public static Class<?> test3() {
		return CallTrace.traceCall(-1);
	}
	
	static class TesterTwo extends CyanComponent {
		public static Class<?> test() {
			return CallTrace.traceCall(1);
		}
		
		public static Class<?> test3() {
			return CyanComponentTest.test3();
		}
		
		static class TesterThree extends CyanComponent {
			public static Class<?> test() {
				return CallTrace.traceCall();
			}

			static class TesterFour {
				public static Class<?> test() {
					return TesterThree.test2();
				}
			}
			
			public static Class<?> test2() {
				return TesterTwo.test3();
			}
		}

		public static Class<?> test2() {
			return TesterThree.test();
		}
		
	}
	
	@Test
	public void callTraceTest() {
		Class<?> caller = TesterTwo.test();
		Class<?> caller2 = TesterTwo.test2();
		Class<?> caller3 = CallTrace.traceCall(0);
		Class<?> finalTest = TesterTwo.TesterThree.TesterFour.test();
		assertTrue(caller3.getTypeName().equals(getClass().getTypeName()));
		assertTrue(caller2.getTypeName().equals(getClass().getTypeName()));
		assertTrue(caller.getTypeName().equals(getClass().getTypeName()));	
		assertTrue(finalTest.getTypeName().equals(TesterTwo.TesterThree.TesterFour.class.getTypeName()));
	}
	
}
