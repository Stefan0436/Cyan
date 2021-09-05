package modkit.events.objects.ingame.rendering.layers;

//import java.util.Iterator;

import org.asf.cyan.api.events.extended.EventObject;

/**
 * 
 * Entity Renderer Registry Event Object -- Register your entity layers by using
 * this (ignored for pre-1.17 clients, <b>NOT YET IMPLEMENTED, MODKIT 1.3</b>)
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 * @deprecated Upcoming feature, unused
 * @since 1.3
 *
 */
@Deprecated
public class EntityModelLayerRegistryEventObject extends EventObject {

	/**
	 * 
	 * The target registry type
	 * 
	 * @author Sky Swimmer - AerialWorks Software Foundation
	 * @since 1.2
	 *
	 */
	public static enum EventTarget {
		LAYER_REGISTRY, DEFINITION_REGISTRY
	}
//
//	private Node first;
//	private Node current;
//	
//	private static class Node implements Iterator<Node> {
//		
//		public Node next;
//		// TODO: value
//		
//		@Override
//		public boolean hasNext() {
//			return next != null;
//		}
//
//		@Override
//		public Node next() {
//			return next;
//		}
//
//	}

}
