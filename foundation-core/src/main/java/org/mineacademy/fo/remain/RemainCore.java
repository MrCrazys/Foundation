package org.mineacademy.fo.remain;

import java.util.Collection;
import java.util.List;

import org.mineacademy.fo.exception.FoException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class RemainCore {

	/**
	 * The Google Json instance
	 */
	public final static Gson GSON = new Gson();

	/**
	 * The Google Json instance with pretty printing
	 */
	public final static Gson GSON_PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	// ----------------------------------------------------------------------------------------------------
	// Misc
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Convert the given json into list
	 *
	 * @param json
	 * @return
	 */
	public static List<String> convertJsonToList(String json) {
		return GSON.fromJson(json, List.class);
	}

	/**
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}

	/**
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrows.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
	}

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String convertListToJson(final Collection<String> list) {
		return GSON.toJson(list);
	}
}

/**
 * A wrapper for Spigot
 */
final class SneakyThrows {

	public static void sneaky(final Throwable t) {
		throw SneakyThrows.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}