package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MessengerCore;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find singular [syntax_name] variables.
	 */
	public static final Pattern MESSAGE_VARIABLE_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * The pattern to find simple {syntax} placeholders.
	 */
	public static final Pattern BRACKET_VARIABLE_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	/**
	 * The pattern to find simple {syntax} placeholders starting with {rel_} (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_VARIABLE_PATTERN = Pattern.compile("[({)](rel_)([^}]+)[(})]");

	/**
	 * If we should replace JavaScript variables
	 */
	@Getter
	@Setter
	private static boolean replaceScript = true;

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * This is used to dynamically replace the variable based on its content, like
	 * PlaceholderAPI.
	 *
	 * We also hook into PlaceholderAPI, however, you'll have to use your plugin's prefix before
	 * all variables when called from there.
	 */
	@Getter
	private static final Set<SimpleExpansion> expansions = new HashSet<>();

	/**
	 * Registers a new expansion if it was not already registered
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		expansions.add(expansion);
	}

	/**
	 * Set the collector to collect variables for the specified audience
	 */
	@Setter
	private static Collector collector = null;

	/**
	 * Collects variables for the specified audience
	 */
	public interface Collector {
		void addVariables(String variable, FoundationPlayer audience, Map<String, Object> replacements);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	public static List<String> replaceListArray(List<String> list, FoundationPlayer sender, Object... replacements) {
		return Arrays.asList(replaceArray(String.join("%FLPV%", list), sender, replacements).split("%FLPV%"));
	}

	public static String[] replaceListArray(String[] list, FoundationPlayer sender, Object... replacements) {
		return replaceArray(String.join("%FLPV%", list), sender, replacements).split("%FLPV%");
	}

	public static List<String> replaceListArray(List<String> list, Object... replacements) {
		return Arrays.asList(replaceArray(String.join("%FLPV%", list), null, replacements).split("%FLPV%"));
	}

	public static String[] replaceListArray(String[] list, Object... replacements) {
		return replaceArray(String.join("%FLPV%", list), null, replacements).split("%FLPV%");
	}

	public static String replaceArray(String list, Object... replacements) {
		return replace(list, null, CommonCore.newHashMap(replacements));
	}

	public static String replaceArray(String list, FoundationPlayer sender, Object... replacements) {
		return replace(list, sender, CommonCore.newHashMap(replacements));
	}

	public static SimpleComponent replaceArray(SimpleComponent list, Object... replacements) {
		return replace(list, null, CommonCore.newHashMap(replacements));
	}

	public static String replace(String message, FoundationPlayer sender) {
		return replace(message, sender, new HashMap<>());
	}

	public static String replace(String message, FoundationPlayer sender, Map<String, Object> replacements) {
		final Matcher matcher = BRACKET_VARIABLE_PATTERN.matcher(message);

		while (matcher.find()) {
			final String variable = matcher.group();
			final SimpleComponent value = replaceVariable(variable, sender, replacements);

			if (value != null)
				message = message.replace(variable, value.toLegacy());
		}

		return message;
	}

	public static SimpleComponent replace(SimpleComponent message, FoundationPlayer sender) {
		return replace(message, sender, new HashMap<>());
	}

	public static SimpleComponent replace(SimpleComponent message, FoundationPlayer sender, Map<String, Object> replacements) {
		return message.replaceMatch(BRACKET_VARIABLE_PATTERN, (result, input) -> {
			final String variable = result.group();
			final SimpleComponent value = replaceVariable(variable, sender, replacements);

			return value == null ? SimpleComponent.fromPlain(variable) : value;
		});
	}

	// TODO measure performance and readd cache
	private static SimpleComponent replaceVariable(String variable, FoundationPlayer audience, @NonNull Map<String, Object> replacements) {
		boolean frontSpace = false;
		boolean backSpace = false;

		if (variable.startsWith("{") && variable.endsWith("}"))
			variable = variable.substring(1, variable.length() - 1);

		if (variable.startsWith("+")) {
			variable = variable.substring(1);

			frontSpace = true;
		}

		if (variable.endsWith("+")) {
			variable = variable.substring(0, variable.length() - 1);

			backSpace = true;
		}

		// Replace custom expansions
		if (audience != null && !Platform.isPlaceholderAPIHooked()) // TODO test if it works with PAPI still
			for (final SimpleExpansion expansion : expansions) {
				SimpleComponent value = expansion.replacePlaceholders(audience, variable);

				if (value != null) {
					if (value.isEmpty())
						return SimpleComponent.empty();

					if (frontSpace)
						value = SimpleComponent.fromPlain(" ").append(value);

					if (backSpace)
						value = value.append(SimpleComponent.fromPlain(" "));

					return value;
				}
			}

		replacements.put("prefix_plugin", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("prefix_info", MessengerCore.getInfoPrefix());
		replacements.put("prefix_success", MessengerCore.getSuccessPrefix());
		replacements.put("prefix_warn", MessengerCore.getWarnPrefix());
		replacements.put("prefix_error", MessengerCore.getErrorPrefix());
		replacements.put("prefix_question", MessengerCore.getQuestionPrefix());
		replacements.put("prefix_announce", MessengerCore.getAnnouncePrefix());
		replacements.put("plugin_name", Platform.getPlugin().getName());
		replacements.put("plugin_version", Platform.getPlugin().getVersion());
		replacements.put("server_name", Platform.hasCustomServerName() ? Platform.getCustomServerName() : "");
		replacements.put("date", TimeUtil.getFormattedDate());
		replacements.put("date_short", TimeUtil.getFormattedDateShort());
		replacements.put("date_month", TimeUtil.getFormattedDateMonth());
		replacements.put("chat_line", CommonCore.chatLine());
		replacements.put("chat_line_smooth", CommonCore.chatLineSmooth());
		replacements.put("label", Platform.getPlugin().getDefaultCommandGroup() != null ? Platform.getPlugin().getDefaultCommandGroup().getLabel() : Lang.legacy("none"));

		replacements.put("sender_is_discord", audience != null && audience.isDiscord() ? "true" : "false");
		replacements.put("sender_is_console", audience != null && audience.isConsole() ? "true" : "false");

		// Replace JavaScript variables
		if (audience != null) {
			if (replaceScript) {
				final Variable javascriptKey = Variable.findVariable(variable, Variable.Type.FORMAT);

				if (javascriptKey != null) {
					final SimpleComponent value = javascriptKey.build(audience, replacements);

					replacements.put(variable, value);
				}
			}

			if (collector != null)
				collector.addVariables(variable, audience, replacements);
		}

		// Finally, do replace
		for (final Map.Entry<String, Object> entry : replacements.entrySet()) {
			final String key = entry.getKey();

			if (key.equals(variable)) {
				final Object valueRaw = entry.getValue();

				ValidCore.checkBoolean(!key.startsWith("{"), "Variable key cannot start with {, found: " + key);
				ValidCore.checkBoolean(!key.endsWith("}"), "Variable key cannot end with }, found: " + key);

				if (valueRaw == null)
					return SimpleComponent.empty();

				SimpleComponent value = null;

				if (valueRaw instanceof SimpleComponent)
					value = (SimpleComponent) valueRaw;

				else if (valueRaw instanceof Collection)
					value = SimpleComponent.fromSection(CommonCore.joinAnd((Collection<?>) valueRaw));

				else if (valueRaw.getClass().isArray())
					value = SimpleComponent.fromSection(CommonCore.joinAnd(Arrays.asList((Object[]) valueRaw)));

				else
					value = SimpleComponent.fromMini(valueRaw.toString());

				if (frontSpace)
					value = SimpleComponent.fromPlain(" ").append(value);

				if (backSpace)
					value = value.append(SimpleComponent.fromPlain(" "));

				return value;
			}
		}

		return null;
	}
}
