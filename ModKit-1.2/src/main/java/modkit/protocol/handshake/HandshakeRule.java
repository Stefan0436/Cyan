package modkit.protocol.handshake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;

import modkit.util.CheckString;

/**
 * 
 * Handshake Rules -- Rules for checking mod server and client dependencies
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since 1.1
 *
 */
public class HandshakeRule {
	private static ArrayList<HandshakeRule> rules = new ArrayList<HandshakeRule>();

	private GameSide side;
	private String key;
	private String checkString;

	public HandshakeRule(GameSide side, String key, String checkString) {
		this.side = side;
		this.key = key;
		this.checkString = checkString;
	}

	public GameSide getSide() {
		return side;
	}

	public String getKey() {
		return key;
	}

	public String getCheckString() {
		return checkString;
	}

	/**
	 * Matches the given entry map with the given rules, true if successful, false
	 * otherwise.
	 * 
	 * @param entries Entry map
	 * @param side    Game side
	 * @param output  Output map
	 * @param rules   Rules to use
	 * @return True if successful, false otherwise
	 */
	public static boolean checkAll(Map<String, Version> entries, GameSide side, Map<String, String> output,
			List<HandshakeRule> rules) {
		if (rules.size() == 0)
			return true;

		boolean fail = false;
		for (HandshakeRule rule : rules) {
			if (!rule.getSide().equals(side))
				continue;

			if (!entries.containsKey(rule.key)) {
				String msg = CheckString.validateCheckString(rule.checkString, Version.fromString(""), "", true);
				if (msg == null)
					msg = "";
				if (msg.equals(" (incompatible)"))
					msg = "   ";
				output.put(rule.key, msg.substring(2, msg.length() - 1));
				fail = true;
				continue;
			}

			String msg = CheckString.validateCheckString(rule.checkString, entries.get(rule.key), "", true);
			if (msg != null) {
				output.put(rule.key, msg.substring(2, msg.length() - 1));
				fail = true;
			}
		}
		if (fail)
			return false;

		return true;
	}

	/**
	 * Registers handshake rules for client/server mod checking
	 * 
	 * @param rule Rule to register
	 */
	public static void registerRule(HandshakeRule rule) {
		if (!rules.stream().anyMatch(t -> t.checkString.equals(rule.getCheckString())
				&& t.getKey().equals(rule.getKey()) && t.getSide() == rule.getSide())) {
			rules.add(rule);
		}
	}

	/**
	 * Retrieves a list of all registered rules
	 */
	public static List<HandshakeRule> getAllRules() {
		return List.copyOf(rules);
	}
}
