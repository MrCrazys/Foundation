package org.mineacademy.fo;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.ConfigStringSerializable;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.RemainCore;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * Our main utility class hosting a large variety of different convenience functions
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CommonCore {

	@Setter
	private static Function<Object, String> simplifier = t -> t.toString();

	/**
	 * Used to send messages to player without repetition, e.g. if they attempt to break a block
	 * in a restricted region, we will not spam their chat with "You cannot break this block here" 120x times,
	 * instead, we only send this message once per X seconds. This cache holds the last times when we
	 * sent that message so we know how long to wait before the next one.
	 */
	private static final Map<SimpleComponent, Long> TIMED_TELL_CACHE = new LinkedHashMap<>();

	/**
	 * See {@link #TIMED_TELL_CACHE}, but this is for sending messages to your console
	 */
	private static final Map<String, Long> TIMED_LOG_CACHE = new LinkedHashMap<>();

	// ------------------------------------------------------------------------------------------------------------
	// Plugin prefixes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The log prefix applied on log() methods, defaults to [PluginName]
	 */
	@Getter
	private static String logPrefix = "";

	/**
	 * Set the log prefix applied for messages in the console from log() methods.
	 *
	 * Colors with & letter are translated automatically.
	 *
	 * Set to "none" to disable.
	 *
	 * @param prefix
	 */
	public static final void setLogPrefix(final String prefix) {
		logPrefix = prefix == null ? "" : prefix;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param messages
	 */
	public static final void broadcast(final String... messages) {
		for (final String message : messages)
			broadcast(SimpleComponent.fromMini(message));
	}

	/**
	 * Broadcast the message to everyone and logs it
	 *
	 * @param message
	 */
	public static final void broadcast(final SimpleComponent message) {
		message.send(Platform.getOnlinePlayers());

		log(message.toLegacy());
	}

	/**
	 * Sends messages to all recipients
	 *
	 * @param recipients
	 * @param message
	 */
	public static final void broadcastTo(final Iterable<FoundationPlayer> recipients, final SimpleComponent message) {
		for (final FoundationPlayer recipient : recipients)
			recipient.sendMessage(message);
	}

	/**
	 * Broadcast the text component message to everyone with permission
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static final void broadcastWithPerm(final String showPermission, @NonNull final String message, final boolean log) {
		broadcastWithPerm(showPermission, SimpleComponent.fromMini(message), log);
	}

	/**
	 * Broadcast the text component message to everyone with permission
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static final void broadcastWithPerm(final String showPermission, @NonNull final SimpleComponent message, final boolean log) {
		if (!message.isEmpty()) {
			for (final FoundationPlayer online : Platform.getOnlinePlayers())
				if (online.hasPermission(showPermission))
					online.sendMessage(message);

			if (log)
				log(message.toLegacy());
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 *
	 * Sends a message to the audience. Supports {prefix_plugin} and {player} variable.
	 * Supports \<actionbar\>, \<toast\>, \<title\>, \<bossbar\> and \<center\>.
	 * Properly sends the message to the player if he is conversing with the server.
	 *
	 * @param audience
	 * @param messages
	 */
	public static final void tell(@NonNull FoundationPlayer audience, String... messages) {
		for (final String message : messages)
			SimpleComponent.fromMini(message).send(audience);
	}

	/**
	* Sends a message to the player and saves the time when it was sent.
	* The delay in seconds is the delay between which we won't send player the
	* same message, in case you call this method again.
	*
	* @param delaySeconds
	* @param sender
	* @param message
	*/
	public static final void tellTimed(final int delaySeconds, final FoundationPlayer sender, final String message) {
		tellTimed(delaySeconds, sender, SimpleComponent.fromMini(message));
	}

	/**
	* Sends a message to the player and saves the time when it was sent.
	* The delay in seconds is the delay between which we won't send player the
	* same message, in case you call this method again.
	*
	* @param delaySeconds
	* @param sender
	* @param message
	*/
	public static final void tellTimed(final int delaySeconds, final FoundationPlayer sender, final SimpleComponent message) {

		// No previous message stored, just tell the player now
		if (!TIMED_TELL_CACHE.containsKey(message)) {
			sender.sendMessage(message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_TELL_CACHE.get(message) > delaySeconds) {
			sender.sendMessage(message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
		}
	}

	/**
	 * Sends a message to the sender with a given delay, colors & are supported
	 *
	 * @param sender
	 * @param delayTicks
	 * @param message
	 */
	public static final void tellLater(final int delayTicks, final FoundationPlayer sender, final String message) {
		Platform.runTask(delayTicks, () -> {
			if (sender.isOnline())
				sender.sendMessage(SimpleComponent.fromMini(message));
		});
	}

	/**
	 * Sends a message to the sender with a given delay, colors & are supported
	 *
	 * @param sender
	 * @param delayTicks
	 * @param message
	 */
	public static final void tellLater(final int delayTicks, final FoundationPlayer sender, final SimpleComponent message) {
		Platform.runTask(delayTicks, () -> {
			if (sender.isOnline())
				sender.sendMessage(message);
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Logging and error handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Logs the message, and saves the time it was logged. If you call this method
	 * to log exactly the same message within the delay in seconds, it will not be logged.
	 * <p>
	 * Saves console spam.
	 *
	 * @param delaySec
	 * @param msg
	 */
	public static final void logTimed(final int delaySec, final String msg) {
		if (!TIMED_LOG_CACHE.containsKey(msg)) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_LOG_CACHE.get(msg) > delaySec) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
		}
	}

	/**
	 * A dummy helper method adding "&cWarning: &f" to the given message
	 * and logging it.
	 *
	 * @param message
	 */
	public static final void warning(String message) {
		log("&cWarning: &7" + message);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param message
	 */
	public static final void log(final SimpleComponent message) {
		log(message.toLegacy());
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static final void log(final List<String> messages) {
		log(toArray(messages));
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static final void log(final String... messages) {
		log(true, messages);
	}

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 * <p>
	 * Does not add {@link #getLogPrefix()}
	 *
	 * @param messages
	 */
	public static final void logNoPrefix(final String... messages) {
		log(false, messages);
	}

	/*
	 * Logs a bunch of messages to the console, & colors are supported
	 */
	private static final void log(final boolean addLogPrefix, final String... messages) {
		if (messages == null)
			return;

		for (final String message : messages) {
			if (message == null || "none".equals(message))
				continue;

			if (message.replace(" ", "").isEmpty()) {
				Platform.logRaw("  ");

				continue;
			}

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					log(SimpleComponent.fromAdventureJson(stripped).toLegacy());

			} else
				for (final String part : message.split("\n"))
					Platform.logRaw(CompChatColor.translateColorCodes((addLogPrefix && !logPrefix.isEmpty() && !logPrefix.equals("none") ? logPrefix + " " : "") + part));
		}
	}

	/**
	 * Logs a bunch of messages to the console in a frame.
	 *
	 * @param messages
	 */
	public static final void logFramed(final String... messages) {
		logFramed(false, messages);
	}

	/**
	 * Logs a bunch of messages to the console in a frame.
	 * <p>
	 * Used when an error occurs, can also disable the plugin
	 *
	 * @param disablePlugin
	 * @param messages
	 */
	public static final void logFramed(final boolean disablePlugin, final String... messages) {
		if (messages != null && !ValidCore.isNullOrEmpty(messages)) {
			log("&7" + chatLine());
			for (final String msg : messages)
				log(" &c" + msg);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + chatLine());
		}

		if (disablePlugin)
			Platform.getPlugin().disable();
	}

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param throwable
	 * @param messages
	 */
	public static final void error(@NonNull Throwable throwable, String... messages) {

		if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
			throwable = throwable.getCause();

		if (!(throwable instanceof FoException))
			Debugger.saveError(throwable, messages);

		Debugger.printStackTrace(throwable);
		logFramed(replaceErrorVariable(throwable, messages));
	}

	/**
	 * Logs the messages in frame (if not null),
	 * saves the error to errors.log and then throws it
	 * <p>
	 * Possible to use %error variable
	 *
	 * @param throwable
	 * @param messages
	 */
	public static final void throwError(Throwable throwable, final String... messages) {
		if (throwable instanceof FoException)
			throw (FoException) throwable;

		Throwable cause = throwable;

		while (cause.getCause() != null)
			cause = cause.getCause();

		// Delegate to only print out the relevant stuff
		if (cause instanceof FoException)
			throw (FoException) throwable;

		if (messages != null)
			logFramed(false, replaceErrorVariable(throwable, messages));

		Debugger.saveError(throwable, messages);
		RemainCore.sneaky(throwable);
	}

	/*
	 * Replace the %error variable with a smart error info, see above
	 */
	private static String[] replaceErrorVariable(Throwable throwable, final String... msgs) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < msgs.length; i++) {
			final String error = throwableName + throwableMessage;

			msgs[i] = msgs[i]
					.replace("%error%", error)
					.replace("%error", error);
		}

		return msgs;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Aesthetics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a long -------- chat line
	 *
	 * @return
	 */
	public static final String chatLine() {
		return "*---------------------------------------------------*";
	}

	/**
	 * Returns a long &m----------- chat line with strike effect
	 *
	 * @return
	 */
	public static final String chatLineSmooth() {
		return "&m-----------------------------------------------------";
	}

	/**
	 * Returns a very long -------- config line
	 *
	 * @return
	 */
	public static final String configLine() {
		return "-------------------------------------------------------------------------------------------";
	}

	/**
	 * A very simple helper for duplicating the given text the given amount of times.
	 *
	 * Example: duplicate("apple", 2) will produce "appleapple"
	 *
	 * @param text
	 * @param nTimes
	 * @return
	 */
	public static final String duplicate(String text, int nTimes) {
		if (nTimes == 0)
			return "";

		final String toDuplicate = new String(text);

		for (int i = 1; i < nTimes; i++)
			text += toDuplicate;

		return text;
	}

	/**
	 * Limits the string to the given length maximum
	 * appending "..." at the end when it is cut
	 *
	 * @param text
	 * @param maxLength
	 * @return
	 */
	public static final String limit(String text, int maxLength) {
		final int length = text.length();

		return maxLength >= length ? text : text.substring(0, maxLength) + "...";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Regular expressions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Compiles a pattern from the given regex, stripping colors and making
	 * it case insensitive
	 *
	 * @param regex
	 * @return
	 */
	public static final Pattern compilePattern(String regex) {
		regex = Platform.getPlugin().isRegexStrippingColors() ? CompChatColor.stripColorCodes(regex) : regex;
		regex = Platform.getPlugin().isRegexStrippingAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

		if (Platform.getPlugin().isRegexCaseInsensitive())
			return Pattern.compile(regex, Platform.getPlugin().isRegexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

		else
			return Platform.getPlugin().isRegexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Tab completing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return a list of tab completions for the given array,
	 * we attempt to resolve what type of the array it is,
	 * supports for chat colors, command senders, enumerations etc.
	 *
	 * @param <T>
	 * @param partialName
	 * @param elements
	 * @return
	 */
	@SafeVarargs
	public static <T> List<String> tabComplete(String partialName, T... elements) {
		final List<String> toComplete = new ArrayList<>();

		if (elements != null)
			for (final T element : elements)
				if (element != null)
					if (element instanceof Iterable)
						for (final Object iterable : (Iterable<?>) element) {
							final String parsedValue = SerializeUtilCore.serialize(Language.YAML, iterable).toString();

							toComplete.add(ReflectionUtilCore.isEnumOrKeyed(iterable) ? parsedValue.toLowerCase() : parsedValue);
						}

					else if (element.getClass().isArray())
						for (int i = 0; i < Array.getLength(element); i++) {
							final Object iterable = Array.get(element, i);
							final String parsedValue = SerializeUtilCore.serialize(Language.YAML, iterable).toString();

							toComplete.add(ReflectionUtilCore.isEnumOrKeyed(iterable) ? parsedValue.toLowerCase() : parsedValue);
						}

					// Trick: Automatically parse enum constants
					else if (element instanceof Enum[])
						for (final Object iterable : ((Enum[]) element)[0].getClass().getEnumConstants())
							toComplete.add(iterable.toString().toLowerCase());

					else {
						final boolean lowercase = ReflectionUtilCore.isEnumOrKeyed(element);
						final String parsedValue = SerializeUtilCore.serialize(Language.YAML, element).toString();

						if (!"".equals(parsedValue))
							toComplete.add(lowercase ? parsedValue.toLowerCase() : parsedValue);
					}

		partialName = partialName.toLowerCase();

		for (final Iterator<String> iterator = toComplete.iterator(); iterator.hasNext();) {
			final String val = iterator.next();

			if (!val.toLowerCase().startsWith(partialName))
				iterator.remove();
		}

		Collections.sort(toComplete);

		return toComplete;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Joins the given array with with "," and "and" of the last element.
	 *
	 * @param list
	 * @return
	 */
	public static final String joinAnd(Collection<?> list) {
		final List<String> simplified = new ArrayList<>();

		for (final Object element : list)
			simplified.add(simplify(element));

		return joinAnd(simplified.toArray(new String[simplified.size()]));
	}

	/**
	 * Joins the given array with with "," and "and" of the last element.
	 *
	 * @param array
	 * @return
	 */
	public static final String joinAnd(String... array) {
		if (array.length == 0)
			return "";

		if (array.length == 1)
			return array[0];

		if (array.length == 2)
			return array[0] + " " + Lang.plain("and") + " " + array[1];

		final StringBuilder out = new StringBuilder();

		for (int i = 0; i < array.length; i++) {
			if (i == array.length - 1)
				out.append(" " + Lang.plain("and") + " ").append(array[i]);
			else
				out.append(i == 0 ? "" : ", ").append(array[i]);
		}

		return out.toString();
	}

	/**
	 * Joins an array of lists together into one big list
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	@SafeVarargs
	public static final <T> List<T> joinLists(final Iterable<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Iterable<T> array : arrays)
			for (final T element : array)
				all.add(element);

		return all;
	}

	/**
	 * Joins an array together using spaces from the given start index
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static final String joinRange(final int startIndex, final String[] array) {
		return joinRange(startIndex, array.length, array);
	}

	/**
	 * Join an array together using spaces using the given range
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @param array
	 * @return
	 */
	public static final String joinRange(final int startIndex, final int stopIndex, final String[] array) {
		return joinRange(startIndex, stopIndex, array, " ");
	}

	/**
	 * Join an array together using the given deliminer
	 *
	 * @param start
	 * @param stop
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static final String joinRange(final int start, final int stop, final String[] array, final String delimiter) {
		String joined = "";

		for (int i = start; i < MathUtilCore.range(stop, 0, array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * A convenience method for converting array of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> String join(final T[] array) {
		return array == null ? "" : join(Arrays.asList(array));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> String join(final Iterable<T> array) {
		return array == null ? "" : join(array, ", ");
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static final <T> String join(final T[] array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static final <T> String join(final Iterable<T> array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * Joins an array of a given type using the ", " delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final T[] array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
	}

	/**
	 * Joins an array of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final T[] array, final String delimiter, final Stringer<T> stringer) {
		ValidCore.checkNotNull(array, "Cannot join null array!");

		return join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * Joins a list of a given type using the comma delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final Iterable<T> array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
	}

	/**
	 * Joins a list of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static final <T> String join(final Iterable<T> array, final String delimiter, final Stringer<T> stringer) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (next != null)
				message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
	}

	/**
	 * Replace some common classes such as entity to name automatically
	 *
	 * @param arg
	 * @return
	 */
	public static final String simplify(Object arg) {
		if (arg == null)
			return "";

		else if (arg instanceof String)
			return (String) arg;

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return MathUtilCore.formatTwoDigits((double) arg);

		else if (arg instanceof Collection)
			return CommonCore.join((Collection<?>) arg, ", ", CommonCore::simplify);

		else if (arg instanceof CompChatColor)
			return ((CompChatColor) arg).getName();

		else if (arg instanceof Enum)
			return ((Enum<?>) arg).toString().toLowerCase();

		else if (arg instanceof FoundationPlayer)
			return ((FoundationPlayer) arg).getName();

		else if (arg instanceof ConfigStringSerializable)
			return ((ConfigStringSerializable) arg).serialize();

		return simplifier.apply(arg);
	}

	/**
	 * Dynamically populates pages, used for pagination in commands or menus
	 *
	 * @param <T>
	 * @param cellSize
	 * @param items
	 * @return
	 */
	public static final <T> Map<Integer, List<T>> fillPages(int cellSize, Iterable<T> items) {
		final List<T> allItems = new ArrayList<>();

		for (final T iterable : items)
			allItems.add(iterable);

		final Map<Integer, List<T>> pages = new LinkedHashMap<>();
		final int pageCount = allItems.size() == cellSize ? 0 : allItems.size() / cellSize;

		for (int i = 0; i <= pageCount; i++) {
			final List<T> pageItems = new ArrayList<>();

			final int down = cellSize * i;
			final int up = down + cellSize;

			for (int valueIndex = down; valueIndex < up; valueIndex++)
				if (valueIndex < allItems.size()) {
					final T page = allItems.get(valueIndex);

					pageItems.add(page);
				} else
					break;

			// If the menu is completely empty, at least allow the first page
			if (i == 0 || !pageItems.isEmpty())
				pages.put(i, pageItems);
		}

		return pages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting and retyping
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the last key in the list or null if list is null or empty
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static final <T> T last(List<T> list) {
		return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * Return the last key in the array or null if array is null or empty
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> T last(T[] array) {
		return array == null || array.length == 0 ? null : array[array.length - 1];
	}

	/**
	 * Converts a list having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter;
	 * @return the new list
	 */
	public static final <Old, New> List<New> convertList(final Iterable<Old> list, final TypeConverter<Old, New> converter) {
		final List<New> copy = new ArrayList<>();

		for (final Old old : list) {
			final New result = converter.convert(old);

			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Attempts to convert an array into a different type
	 *
	 * @param <Old>
	 * @param <New>
	 * @param oldArray
	 * @param converter
	 * @return
	 */
	public static final <Old, New> List<New> convertArrayToList(final Old[] oldArray, final TypeConverter<Old, New> converter) {
		final List<New> newList = new ArrayList<>();

		for (final Old old : oldArray)
			newList.add(converter.convert(old));

		return newList;
	}

	/**
	 * Converts a set having one type object into another
	 *
	 * @param list      the old list
	 * @param converter the converter;
	 * @return the new list
	 */
	public static final <Old, New> Set<New> convertSet(final Iterable<Old> list, final TypeConverter<Old, New> converter) {
		final Set<New> copy = new HashSet<>();

		for (final Old old : list) {
			final New result = converter.convert(old);

			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * Attempts to convert the given map into another map
	 *
	 * @param <OldK>
	 * @param <OldV>
	 * @param <NewK>
	 * @param <NewV>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static final <OldK, OldV, NewK, NewV> Map<NewK, NewV> convertMap(final Map<OldK, OldV> oldMap, final MapToMapConverter<OldK, OldV, NewK, NewV> converter) {
		final Map<NewK, NewV> newMap = new LinkedHashMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * Attempts to convert the gfiven map into a list
	 *
	 * @param <ListKey>
	 * @param <OldK>
	 * @param <OldV>
	 * @param map
	 * @param converter
	 * @return
	 */
	public static final <ListKey, OldK, OldV> List<ListKey> convertMapToList(final Map<OldK, OldV> map, final MapToListConverter<ListKey, OldK, OldV> converter) {
		final List<ListKey> list = new ArrayList<>();

		for (final Map.Entry<OldK, OldV> e : map.entrySet())
			list.add(converter.convert(e.getKey(), e.getValue()));

		return list;
	}

	/**
	 * Split the given string into array of the given max line length
	 *
	 * @param input
	 * @param maxLineLength
	 * @return
	 */
	public static final String[] split(String input, int maxLineLength) {
		final StringTokenizer tok = new StringTokenizer(input, " ");
		final StringBuilder output = new StringBuilder(input.length());
		int lineLen = 0;
		String lastColorCode = "";

		while (tok.hasMoreTokens()) {
			final String word = tok.nextToken();

			if (lineLen + word.length() > maxLineLength) {
				output.append("\n").append(lastColorCode);

				lineLen = 0;
			}

			final String colorCode = CompChatColor.getLastColors(word);

			if (!colorCode.isEmpty())
				lastColorCode = colorCode;

			output.append(word).append(" ");
			lineLen += word.length() + 1;
		}

		return output.toString().split("\n");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new list only containing non-null and not empty string elements
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static final <T> List<T> removeNullAndEmpty(final List<T> list) {
		final List<T> copy = new ArrayList<>();

		for (final T key : list)
			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);

		return copy;
	}

	/**
	 * Replace all nulls with an empty string
	 *
	 * @param list
	 * @return
	 */
	public static final String[] replaceNullWithEmpty(final String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
	}

	/**
	 * Return an empty String if the String is null or equals to none.
	 *
	 * @param input
	 * @return
	 */
	public static final String getOrEmpty(final String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * If the String equals to none or is empty, return null
	 *
	 * @param input
	 * @return
	 */
	public static final String getOrNull(final String input) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? null : input;
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * PSA: If values are strings, we return default if the value is empty or equals to "none"
	 *
	 * @param value the primary value
	 * @param def   the default value
	 * @return the value, or default it the value is null
	 */
	public static final <T> T getOrDefault(final T value, final T def) {
		if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
			return def;

		return getOrDefaultStrict(value, def);
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * @param <T>
	 * @param value
	 * @param def
	 * @return
	 */
	public static final <T> T getOrDefaultStrict(final T value, final T def) {
		return value != null ? value : def;
	}

	/**
	 * Get next element in the list increasing the index by 1 if forward is true,
	 * or decreasing it by 1 if it is false
	 *
	 * @param <T>
	 * @param given
	 * @param list
	 * @param forward
	 * @return
	 */
	public static final <T> T getNext(final T given, final List<T> list, final boolean forward) {
		if (given == null && list.isEmpty())
			return null;

		final T[] array = (T[]) Array.newInstance((given != null ? given : list.get(0)).getClass(), list.size());

		for (int i = 0; i < list.size(); i++)
			Array.set(array, i, list.get(i));

		return getNext(given, array, forward);
	}

	/**
	 * Get next element in the list increasing the index by 1 if forward is true,
	 * or decreasing it by 1 if it is false
	 *
	 * @param <T>
	 * @param given
	 * @param array
	 * @param forward
	 * @return
	 */
	public static final <T> T getNext(final T given, final T[] array, final boolean forward) {
		if (array.length == 0)
			return null;

		int index = 0;

		for (int i = 0; i < array.length; i++) {
			final T element = array[i];

			if (element.equals(given)) {
				index = i;

				break;
			}
		}

		if (index != -1) {
			final int nextIndex = index + (forward ? 1 : -1);

			// Return the first slot if reached the end, or the last if vice versa
			return nextIndex >= array.length ? array[0] : nextIndex < 0 ? array[array.length - 1] : array[nextIndex];
		}

		return null;
	}

	/**
	 * Converts a list of string into a string array
	 *
	 * @param array
	 * @return
	 */
	public static final String[] toArray(final Collection<String> array) {
		return array == null ? new String[0] : array.toArray(new String[array.size()]);
	}

	/**
	 * Creates a new modifiable array list from String array
	 *
	 * @param array
	 * @return
	 */
	public static final List<String> toList(final String... array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Creates a new modifiable array list from array
	 *
	 * @param array
	 * @return
	 */
	public static final <T> ArrayList<T> toList(final T[] array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Reverses elements in the array
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static final <T> T[] reverse(final T[] array) {
		if (array == null)
			return null;

		int i = 0;
		int j = array.length - 1;

		while (j > i) {
			final T tmp = array[j];

			array[j] = array[i];
			array[i] = tmp;

			j--;
			i++;
		}

		return array;
	}

	/**
	 * Return a new hashmap having the given first key and value pair
	 *
	 * @param <A>
	 * @param <B>
	 * @param firstKey
	 * @param firstValue
	 * @return
	 */
	public static final <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue) {
		final Map<A, B> map = new LinkedHashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	/**
	 * Create a map with multiple keys and values.
	 * The keys and values must be in pairs and of the same type.
	 *
	 * @param <K>
	 * @param entries
	 * @return
	 */
	@SafeVarargs
	public static final <K> Map<K, Object> newHashMap(Object... entries) {
		if (entries == null || entries.length == 0)
			return new LinkedHashMap<>();

		if (entries.length % 2 != 0)
			throw new FoException("Entries must be in pairs: " + Arrays.toString(entries) + ", got " + entries.length + " entries.");

		final Map<K, Object> map = new LinkedHashMap<>();

		final K firstKey = (K) entries[0];

		for (int i = 0; i < entries.length; i += 2) {
			final K key = (K) entries[i];
			final Object value = entries[i + 1];

			if (key == null)
				throw new FoException("Key cannot be null at index " + i);

			if (!firstKey.getClass().isInstance(key))
				throw new FoException("All keys must be a String. Got " + key.getClass().getSimpleName());

			map.put(key, value);
		}

		return map;
	}

	/**
	 * Create a new hashset
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static final <T> Set<T> newSet(final T... keys) {
		return new HashSet<>(Arrays.asList(keys));
	}

	/**
	 * Create a new array list that is mutable (if you call Arrays.asList that is unmodifiable)
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static final <T> List<T> newList(final T... keys) {
		final List<T> list = new ArrayList<>();

		Collections.addAll(list, keys);

		return list;
	}

	/**
	 * Return a map sorted by values (i.e. from smallest to highest for numbers)
	 *
	 * @param map
	 * @return
	 */
	public static final Map<String, Integer> sortByValue(Map<String, Integer> map) {
		final List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
		list.sort(Map.Entry.comparingByValue());

		final Map<String, Integer> sortedMap = new LinkedHashMap<>();

		for (final Map.Entry<String, Integer> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());

		return sortedMap;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Checked sleep method from {@link Thread#sleep(long)} but without the try-catch need
	 *
	 * @param millis
	 */
	public static final void sleep(final int millis) {
		try {
			Thread.sleep(millis);

		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compress the given string into a byte array
	 *
	 * @param data
	 * @return
	 */
	public static final byte[] compress(String data) {
		try {
			final byte[] input = data.getBytes("UTF-8");
			final Deflater deflater = new Deflater();

			deflater.setInput(input);
			deflater.finish();

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length)) {
				final byte[] buffer = new byte[1024];

				while (!deflater.finished()) {
					final int count = deflater.deflate(buffer);

					outputStream.write(buffer, 0, count);
				}

				return outputStream.toByteArray();
			}

		} catch (final Exception ex) {
			CommonCore.throwError(ex, "Failed to compress data");

			return new byte[0];
		}
	}

	/**
	 * Decompress the given byte array into a string
	 *
	 * @param data
	 * @return
	 */
	public static final String decompress(byte[] data) {
		final Inflater inflater = new Inflater();
		inflater.setInput(data);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
			final byte[] buffer = new byte[1024];

			while (!inflater.finished()) {
				final int count = inflater.inflate(buffer);

				outputStream.write(buffer, 0, count);
			}

			return new String(outputStream.toByteArray(), "UTF-8");

		} catch (final Exception ex) {
			CommonCore.throwError(ex, "Failed to decompress data");

			return "";
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A simple interface from converting objects into strings
	 *
	 * @param <T>
	 */
	public interface Stringer<T> {

		/**
		 * Convert the given object into a string
		 *
		 * @param object
		 * @return
		 */
		String toString(T object);
	}

	/**
	 * A simple interface to convert between types
	 *
	 * @param <Old> the initial type to convert from
	 * @param <New> the final type to convert to
	 */
	public interface TypeConverter<Old, New> {

		/**
		 * Convert a type given from A to B
		 *
		 * @param value the old value type
		 * @return the new value type
		 */
		New convert(Old value);
	}

	/**
	 * Convenience class for converting map to a list
	 *
	 * @param <O>
	 * @param <K>
	 * @param <Val>
	 */
	public interface MapToListConverter<O, K, Val> {

		/**
		 * Converts the given map key-value pair into a new type stored in a list
		 *
		 * @param key
		 * @param value
		 * @return
		 */
		O convert(K key, Val value);
	}

	/**
	 * Convenience class for converting between maps
	 *
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface MapToMapConverter<A, B, C, D> {

		/**
		 * Converts the old key type to a new type
		 *
		 * @param key
		 * @return
		 */
		C convertKey(A key);

		/**
		 * Converts the old value into a new value type
		 *
		 * @param value
		 * @return
		 */
		D convertValue(B value);
	}
}