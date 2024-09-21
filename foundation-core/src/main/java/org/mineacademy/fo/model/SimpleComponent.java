package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.FoScriptException;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.RemainCore;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * A very simple way of sending interactive chat messages
 */
public final class SimpleComponent implements ConfigSerializable, ComponentLike {

	/**
	 * Stores legacy colors
	 */
	private static final Map<String, String> LEGACY_TO_MINI = new HashMap<>();

	/**
	 * The pattern for matching MiniMessage tags
	 */
	//private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<[!?#]?[a-z0-9_-]*>");

	static {
		LEGACY_TO_MINI.put("&0", "<black>");
		LEGACY_TO_MINI.put("&1", "<dark_blue>");
		LEGACY_TO_MINI.put("&2", "<dark_green>");
		LEGACY_TO_MINI.put("&3", "<dark_aqua>");
		LEGACY_TO_MINI.put("&4", "<dark_red>");
		LEGACY_TO_MINI.put("&5", "<dark_purple>");
		LEGACY_TO_MINI.put("&6", "<gold>");
		LEGACY_TO_MINI.put("&7", "<gray>");
		LEGACY_TO_MINI.put("&8", "<dark_gray>");
		LEGACY_TO_MINI.put("&9", "<blue>");
		LEGACY_TO_MINI.put("&a", "<green>");
		LEGACY_TO_MINI.put("&b", "<aqua>");
		LEGACY_TO_MINI.put("&c", "<red>");
		LEGACY_TO_MINI.put("&d", "<light_purple>");
		LEGACY_TO_MINI.put("&e", "<yellow>");
		LEGACY_TO_MINI.put("&f", "<white>");
		LEGACY_TO_MINI.put("&n", "<u>");
		LEGACY_TO_MINI.put("&m", "<st>");
		LEGACY_TO_MINI.put("&k", "<obf>");
		LEGACY_TO_MINI.put("&o", "<i>");
		LEGACY_TO_MINI.put("&l", "<b>");
		LEGACY_TO_MINI.put("&r", "<reset>");

		LEGACY_TO_MINI.put("§0", "<black>");
		LEGACY_TO_MINI.put("§1", "<dark_blue>");
		LEGACY_TO_MINI.put("§2", "<dark_green>");
		LEGACY_TO_MINI.put("§3", "<dark_aqua>");
		LEGACY_TO_MINI.put("§4", "<dark_red>");
		LEGACY_TO_MINI.put("§5", "<dark_purple>");
		LEGACY_TO_MINI.put("§6", "<gold>");
		LEGACY_TO_MINI.put("§7", "<gray>");
		LEGACY_TO_MINI.put("§8", "<dark_gray>");
		LEGACY_TO_MINI.put("§9", "<blue>");
		LEGACY_TO_MINI.put("§a", "<green>");
		LEGACY_TO_MINI.put("§b", "<aqua>");
		LEGACY_TO_MINI.put("§c", "<red>");
		LEGACY_TO_MINI.put("§d", "<light_purple>");
		LEGACY_TO_MINI.put("§e", "<yellow>");
		LEGACY_TO_MINI.put("§f", "<white>");
		LEGACY_TO_MINI.put("§n", "<u>");
		LEGACY_TO_MINI.put("§m", "<st>");
		LEGACY_TO_MINI.put("§k", "<obf>");
		LEGACY_TO_MINI.put("§o", "<i>");
		LEGACY_TO_MINI.put("§l", "<b>");
		LEGACY_TO_MINI.put("§r", "<reset>");
	}

	/**
	 * The components we are creating
	 */
	private final List<ConditionalComponent> subcomponents;

	/**
	 * Shall this component ignore empty components? Defaults to false
	 */
	@Getter
	private boolean ignoreEmpty = false;

	/**
	 * The last style used, null if none
	 */
	private Style lastStyle = null;

	private SimpleComponent(List<ConditionalComponent> components) {
		this(components, null);
	}

	private SimpleComponent(List<ConditionalComponent> components, Style lastStyle) {
		this.subcomponents = Collections.unmodifiableList(components);
		this.lastStyle = lastStyle;
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text hover event
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(String... lines) {
		return this.onHover(CommonCore.convertArrayToList(lines, SimpleComponent::fromMini));
	}

	/**
	 * Add a show text event
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(Collection<SimpleComponent> lines) {
		return this.onHover(lines.toArray(new SimpleComponent[lines.size()]));
	}

	/**
	 * Add a hover event
	 *
	 * @param components
	 * @return
	 */
	public SimpleComponent onHover(SimpleComponent[] components) {
		Component joined = Component.empty();

		for (int i = 0; i < components.length; i++) {
			joined = joined.append(components[i]);

			if (i < components.length - 1)
				joined = joined.append(Component.newline());
		}

		final Component finalComponent = joined.asComponent();
		return modifyLastComponentAndReturn(component -> component.hoverEvent(finalComponent));
	}

	/**
	 * Add a hover event
	 *
	 * @param hover
	 * @return
	 */
	public SimpleComponent onHover(HoverEventSource<?> hover) {
		return modifyLastComponentAndReturn(component -> component.hoverEvent(hover));
	}

	/**
	 * Add a run command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(String text) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.runCommand(text)));
	}

	/**
	 * Add a suggest command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(String text) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.suggestCommand(text)));
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(String url) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.openUrl(url)));
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickCopyToClipboard(String url) {
		return modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.copyToClipboard(url)));
	}

	/**
	 * Invoke SimpleComponent setInsertion
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(String insertion) {
		return modifyLastComponentAndReturn(component -> component.insertion(insertion));
	}

	/**
	 * Set the view condition for this component
	 *
	 * @param viewCondition
	 * @return
	 */
	public SimpleComponent viewCondition(String viewCondition) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewCondition(viewCondition);

		return this;
	}

	/**
	 * Set the view permission for this component
	 *
	 * @param viewPermission
	 * @return
	 */
	public SimpleComponent viewPermission(String viewPermission) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewPermission(viewPermission);

		return this;
	}

	/**
	 * Set the view permission for this component
	 *
	 * @param color
	 * @return
	 */
	public SimpleComponent color(TextColor color) {

		// No RGB support in older versions
		if (color instanceof CompChatColor && MinecraftVersion.olderThan(V.v1_16))
			color = NamedTextColor.nearestTo(color);

		final TextColor finalColor = color;
		return modifyLastComponentAndReturn(component -> component.color(finalColor));
	}

	/**
	 * Set the view permission for this component
	 *
	 * @param color
	 * @return
	 */
	public SimpleComponent decoration(TextDecoration color) {
		return modifyLastComponentAndReturn(component -> component.decoration(color, true));
	}

	/**
	 * Quickly replaces an object in all parts of this component, adding
	 * {} around it.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceBracket(String variable, String value) {
		return this.replaceBracket(variable, fromPlain(value));
	}

	/**
	 * Quickly replaces an object in all parts of this component, adding
	 * {} around it.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceBracket(String variable, SimpleComponent value) {
		return this.replaceLiteral("{" + variable + "}", value);
	}

	/**
	 * Quickly replaces an object in all parts of this component
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceLiteral(String variable, String value) {
		return this.replaceLiteral(variable, fromPlain(value));
	}

	/**
	 * Quickly replaces an object in all parts of this component
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public SimpleComponent replaceLiteral(String variable, SimpleComponent value) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.matchLiteral(variable).replacement(value));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public SimpleComponent replaceMatch(Pattern pattern, BiFunction<MatchResult, TextComponent.Builder, ComponentLike> replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public SimpleComponent replaceMatch(Pattern pattern, String replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public SimpleComponent replaceMatch(Pattern pattern, SimpleComponent replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendPlain(String text) {
		return this.append(fromPlain(text));
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendSection(String text) {
		return this.append(fromSection(text));
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent appendMini(String text) {
		return this.append(fromMini(text));
	}

	/**
	 * Append a new line on the end of this component
	 *
	 * @return
	 */
	public SimpleComponent appendNewLine() {
		return this.appendPlain("\n");
	}

	/**
	 * Append a new simple component
	 *
	 * @param newComponent
	 * @return
	 */
	public SimpleComponent append(SimpleComponent newComponent) {

		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent oldSubcomponent : this.subcomponents)
			copy.add(oldSubcomponent);

		for (int i = 0; i < newComponent.subcomponents.size(); i++) {
			final ConditionalComponent newSubcomponent = newComponent.subcomponents.get(i);
			Component adventure = newSubcomponent.getComponent();

			// Why I prefer legacy over Adventure > last style is not properly kept, i.e. "&c[Prefix]&7" resets the gray, so we have
			// to manually save it and reapply it later
			if (i == 0 && this.lastStyle != null)
				adventure = adventure.style(this.lastStyle);

			copy.add(new ConditionalComponent(adventure, newSubcomponent.getViewPermission(), newSubcomponent.getViewCondition()));
		}

		return new SimpleComponent(copy);
	}

	/**
	 * Return if this component is empty
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.subcomponents.isEmpty() || this.toPlain().isEmpty();
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String toLegacy() {

		// Append tail from the last style
		String suffix = "";

		if (this.lastStyle != null) {
			if (this.lastStyle.color() != null)
				suffix = CompChatColor.fromString(this.lastStyle.color().asHexString()).toString();

			for (final Map.Entry<TextDecoration, State> entry : this.lastStyle.decorations().entrySet())
				if (entry.getValue() == State.TRUE)
					suffix += CompChatColor.fromString(entry.getKey().name()).toString();
		}

		return LegacyComponentSerializer.legacySection().serialize(this.toAdventure()) + suffix;
	}

	/**
	 * Return the minimessage representation of the component
	 *
	 * @return
	 */
	public String toMini() {
		return MiniMessage.miniMessage().serialize(this.toAdventure());
	}

	/**
	 * Return the plain colorless message combining all components into one
	 * without click/hover events.
	 *
	 * This effectivelly removes all & and § colors as well as MiniMessage tags.
	 *
	 * @return
	 */
	public String toPlain() {
		return PlainTextComponentSerializer.plainText().serialize(this.toAdventure());
	}

	/**
	 *
	 * @return
	 */
	public String toAdventureJson() {
		return GsonComponentSerializer.gson().serialize(this.toAdventure());
	}

	/**
	 * @return
	 */
	@Override
	public Component asComponent() {
		return this.toAdventure();
	}

	/**
	 * Convert into Adventure component
	 *
	 * @return
	 */
	public Component toAdventure() {
		return this.toAdventure(null);
	}

	/**
	 * Convert into Adventure component, executing viewCondition and viewPermission for the given receiver
	 *
	 * @param receiver the given receiver, can be null
	 * @return
	 */
	public Component toAdventure(FoundationPlayer receiver) {
		Component main = null;

		for (final ConditionalComponent part : this.subcomponents) {
			final Component component = part.build(receiver);

			if (component != null) {
				if (main == null)
					main = component;
				else
					main = main.append(component);
			}
		}

		return main;
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param receivers
	 */
	public final void send(FoundationPlayer... receivers) {
		this.send(CommonCore.newList(receivers));
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param receivers
	 */
	public void send(Iterable<FoundationPlayer> receivers) {
		if (this.ignoreEmpty && this.isEmpty())
			return;

		for (final FoundationPlayer receiver : receivers)
			receiver.sendRawMessage(this.toAdventure(receiver));
	}

	/**
	 * Set if this component should ignore empty components? Defaults to false
	 *
	 * @param ignoreEmpty
	 * @return
	 */
	public SimpleComponent setIgnoreEmpty(boolean ignoreEmpty) {
		this.ignoreEmpty = ignoreEmpty;

		return this;
	}

	/*
	 * Helper method to modify the last component
	 */
	protected SimpleComponent modifyLastComponentAndReturn(Function<Component, Component> editor) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (int i = 0; i < this.subcomponents.size(); i++) {
			ConditionalComponent component = this.subcomponents.get(i);

			if (i == this.subcomponents.size() - 1)
				component = ConditionalComponent.fromComponent(editor.apply(component.getComponent()));

			copy.add(component);
		}

		return new SimpleComponent(copy);
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Components", this.subcomponents,
				"Ignore_Empty", this.ignoreEmpty,
				"Last_Style", this.lastStyle);
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof String)
			return this.toPlain().equals(obj);

		if (obj instanceof SimpleComponent) {
			final SimpleComponent other = (SimpleComponent) obj;

			return this.toMini().equals(other.toMini());
		}

		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		throw new FoException("SimpleComponent#toString() is unsupported, use toMini(), toLegacy() or toPlain() as needed");
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @return
	 */
	public static SimpleComponent empty() {
		return new SimpleComponent(CommonCore.newList(ConditionalComponent.fromComponent(Component.empty())));
	}

	/**
	 * Replaces & color codes and MiniMessage tags in the message.
	 * Also replaces {plugin_prefix}, {plugin_name} and {plugin_version} with their respective values.
	 *
	 * @param message
	 * @return
	 */
	public static SimpleComponent fromMini(String message) {
		if (message == null || message.trim().isEmpty())
			return SimpleComponent.empty();

		if (message.startsWith("<center>"))
			message = ChatUtil.center(message.replace("<center>", "").trim());

		// First, replace legacy & color codes
		final StringBuilder result = new StringBuilder();

		for (int i = 0; i < message.length(); i++) {
			if (i + 1 < message.length() && (message.charAt(i) == '&' || message.charAt(i) == '§')) {
				final String code = message.substring(i, i + 2);

				if (LEGACY_TO_MINI.containsKey(code)) {
					result.append(LEGACY_TO_MINI.get(code));
					i++;

					continue;
				}

			} else if (i + 7 < message.length() && message.charAt(i) == '#' && message.substring(i + 1, i + 7).matches("[0-9a-fA-F]{6}")) {
				//result.append("<#").append(message.substring(i + 1, i + 7)).append(">");
				//i += 6;

				//continue;
			}

			result.append(message.charAt(i));
		}

		message = result.toString();
		//message = escapeInvalidTags(message);

		Component mini;

		try {
			mini = MiniMessage.miniMessage().deserialize(message);

		} catch (final Throwable t) {
			Debugger.printStackTrace("Error parsing mini message tags in: " + message);

			RemainCore.sneaky(t);
			return null;
		}

		Style lastStyle = null;

		// if message ends with color code from the above map, add an empty component at the end with the same color
		if (!message.endsWith(" "))
			for (final String value : LEGACY_TO_MINI.values()) {
				if (message.endsWith(value)) {
					lastStyle = MiniMessage.miniMessage().deserialize(value).style();

					mini = Component
							.text("")
							.style(lastStyle)
							.children(Arrays.asList(mini));

					break;
				}
			}

		return new SimpleComponent(CommonCore.newList(ConditionalComponent.fromComponent(mini)), lastStyle);
	}

	/*
	 * Escapes invalid minimessage tags in the message.
	 */
	/*private static String escapeInvalidTags(String input) {
		Matcher matcher = Pattern.compile("<[^>]*>").matcher(input);
		StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			String match = matcher.group(0);

			if (!MINIMESSAGE_PATTERN.matcher(match).matches())
				match = match.replace("<", "\\\\<").replace(">", "\\>");

			matcher.appendReplacement(buffer, match);
		}

		matcher.appendTail(buffer);
		return buffer.toString();
	}*/

	/**
	 * Create a new interactive chat component supporting § variables
	 *
	 * @param legacyText
	 * @return
	 */
	public static SimpleComponent fromSection(@NonNull String legacyText) {
		return new SimpleComponent(CommonCore.newList(ConditionalComponent.fromSection(legacyText)));
	}

	/**
	 * Create a new interactive chat component supporting & and § variables
	 *
	 * @param legacyText
	 * @return
	 */
	public static SimpleComponent fromAmpersand(@NonNull String legacyText) {
		return fromSection(CompChatColor.translateColorCodes(legacyText));
	}

	/**
	 * Create a new interactive chat component
	 *
	 * @param component
	 * @return
	 */
	public static SimpleComponent fromAdventure(@NonNull Component component) {
		return new SimpleComponent(CommonCore.newList(ConditionalComponent.fromComponent(component)));
	}

	/**
	 * Create a new interactive chat component from plain text
	 *
	 * @param plainText
	 * @return
	 */
	public static SimpleComponent fromPlain(@NonNull String plainText) {
		return new SimpleComponent(CommonCore.newList(ConditionalComponent.fromPlain(plainText)));
	}

	/**
	 * CCreate a new interactive chat component from json
	 *
	 * @param json
	 * @return
	 */
	public static SimpleComponent fromJson(@NonNull String json) {
		return deserialize(SerializedMap.of(Language.JSON, json));
	}

	/**
	 * Create a new interactive chat component from children
	 *
	 * @param components
	 * @return
	 */
	public static SimpleComponent fromChildren(SimpleComponent... components) {
		final List<ConditionalComponent> children = new ArrayList<>();

		for (final SimpleComponent component : components)
			children.addAll(component.subcomponents);

		return new SimpleComponent(children);
	}

	/**
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		final List<ConditionalComponent> components = map.getList("Components", ConditionalComponent.class);
		final SimpleComponent component = new SimpleComponent(components);

		component.ignoreEmpty = map.getBoolean("Ignore_Empty");
		component.lastStyle = map.get("Last_Style", Style.class);

		return component;
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	@Setter
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	static final class ConditionalComponent implements ConfigSerializable {

		private final Component component;
		private String viewPermission;
		private String viewCondition;

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Component", MiniMessage.miniMessage().serialize(this.component));
			map.putIf("Permission", this.viewPermission);
			map.putIf("Condition", this.viewCondition);

			return map;
		}

		public static ConditionalComponent deserialize(SerializedMap map) {
			final Component component = MiniMessage.miniMessage().deserialize(map.getString("Component"));
			final ConditionalComponent part = new ConditionalComponent(component);

			part.viewPermission = map.getString("Permission");
			part.viewCondition = map.getString("Condition");

			return part;
		}

		/*
		 * Build the component for the given receiver
		 */
		private Component build(FoundationPlayer receiver) {

			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !receiver.hasPermission(this.viewPermission)))
				return null;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return null;

				try {
					final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);

					if (result != null) {
						ValidCore.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

						if (!((boolean) result))
							return null;
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Failed parsing view condition for component!",
							"",
							"The view condition must be a JavaScript code that returns a boolean!",
							"Component: " + this,
							"Line: " + ex.getErrorLine(),
							"Error: " + ex.getMessage());

					throw ex;
				}
			}

			return this.component;
		}

		@Override
		public String toString() {
			return this.serialize().toStringFormatted();
		}

		/**
		 * Create a new component from a legacy text
		 *
		 * @param text
		 * @return
		 */
		static ConditionalComponent fromSection(String text) {
			return new ConditionalComponent(LegacyComponentSerializer.legacySection().deserialize(text));
		}

		/**
		 * Create a new component from a legacy text
		 *
		 * @param text
		 * @return
		 */
		static ConditionalComponent fromAmpersand(String text) {
			return new ConditionalComponent(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
		}

		/**
		 * Create a new component from a component
		 *
		 * @param component
		 * @return
		 */
		static ConditionalComponent fromComponent(Component component) {
			return new ConditionalComponent(component);
		}

		/**
		 * Create a new component from plain text, fastest
		 *
		 * @param component
		 * @return
		 */
		static ConditionalComponent fromPlain(String plainText) {
			return new ConditionalComponent(Component.text(plainText));
		}
	}

	public interface Builder {
		Component onBuild(FoundationPlayer sender, FoundationPlayer receiver, Component component);
	}
}
