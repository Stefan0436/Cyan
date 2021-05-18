package org.example.mods.examplemod.channels.packets.examplechannel;

import org.asf.cyan.api.events.network.AbstractPacket;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.PacketWriter;

public class HelloWorldPacket extends AbstractPacket<HelloWorldPacket> {

	@Override
	protected String id() {
		return "hello.world"; // Our packet id
	}

	public String playerName;
	public String message;

	@Override
	protected void readEntries(PacketReader reader) {
		// Read the packet entries
		playerName = reader.readString(); // Reads a string entry
		message = reader.readString(); // Reads another string entry
		
		//
		// There are a lot of entry types, some more examples:
		//
		// ArrayList<String> entries = new ArrayList<String>();
		//
		// int count = reader.readInt(); // Reads an integer value
		// for (int i = 0; i < count; i++) {
		//     entries.add(reader.readString()); // Reads a string and adds it to the list
		// }
		// 
		//
		// Map content:
		// HashMap<String, String> mapEntries = new HashMap<String, String>();
		//
		// count = reader.readInt(); // Reads an integer value
		// for (int i = 0; i < count; i++) {
		// 		// Reads the key and value, adds it to the map
		//      mapEntries.put(reader.readString(), reader.readString());
		// }
		//
		// You can also add and read any serializable objects:
		// reader.readObject(ArrayList.class); // Reads a serialized array list
		// 
		// Or cyan packet entries:
		// reader.readEntry();
		//
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		// Writes our packet:
		writer.writeString(playerName); // Writes the player name
		writer.writeString(message); // Writes the message
		
		//
		// The PacketWriter acts as the reverse of the reader,
		// you can convert example code above to the following to send it:
		//
		// ArrayList<String> entries = new ArrayList<String>();
		// entries.add("One");
		// entries.add("Two");
		//
		// Write the list:
		// writer.writeInt(entries.size());
		// for (String entry : entries)
		//  	writer.writeString(entry);
		//
		//
		// For maps, use this:
		// writer.writeInt(entries.size());
		// entries.forEach((key, value) -> {
		// 		writer.writeString(key); // Writes the key
		/// 	writer.writeString(value); // Writes the value
		// });
		//
		// Or simply use the java serializer:
		// writer.writeObject(entries); // Serializes the array list and writes it
		//		
	}

	// Custom method called by our server event
	public AbstractPacket<HelloWorldPacket> setValues(ServerConnectionEventObject event, String message) {
		
		// Set message and player name
		this.message = message;
		this.playerName = event.getPlayer().getName().getString();
		
		return this;
	}

}
