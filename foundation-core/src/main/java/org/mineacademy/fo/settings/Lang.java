package org.mineacademy.fo.settings;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.CaseNumberFormat;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.RemainCore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the new way of internalization, with the greatest
 * upside of saving development time.
 *
 * The downside is that keys are not checked during load so any
 * malformed or missing key will fail later and may be unnoticed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Lang {

	/**
	 * The instance of this class
	 */
	private static final Lang instance = new Lang();

	private final JsonObject dictionary = new JsonObject();

	@Setter
	private static String customLocaleUrl = null;

	@Setter
	@Getter
	private static boolean offlineMode = false;

	public static File dumpLocale() {
		final File localFile = FileUtil.createIfNotExists("lang/" + SimpleSettings.LOCALE + ".json");
		final JsonObject localJson = readJsonFromFile(localFile);

		CommonCore.log("Dumping locale to " + localFile.getAbsolutePath());

		// First, remove local keys that no longer exist in our dictionary
		for (final String key : localJson.keySet())
			if (!instance.dictionary.has(key)) {
				CommonCore.log("Removing unused key '" + key + "'");

				localJson.remove(key);
			}

		// Then, add new keys to the local file
		for (final String key : instance.dictionary.keySet())
			if (!localJson.has(key)) {
				CommonCore.log("Adding new key '" + key + "'");

				localJson.add(key, instance.dictionary.get(key));
			}

		FileUtil.write(localFile, Arrays.asList(RemainCore.GSON_PRETTY.toJson(localJson)), StandardOpenOption.TRUNCATE_EXISTING);

		return localFile;
	}

	public static void downloadLocales() {

		final String english = Locale.US.getLanguage() + "_" + Locale.US.getCountry();

		// Download Foundation base locale
		downloadLocale("https://raw.githubusercontent.com/kangarko/Foundation/refs/heads/v7/foundation-core/src/main/resources/overlay", english, true);

		// If plugin has a custom URL, load the default English locale first
		// If the customLocaleUrl is null, we attempt to load the locale from the internal JAR
		downloadLocale(customLocaleUrl, english, true);

		// If user selected locale is not English, download it and overlay
		if (!SimpleSettings.LOCALE.equals("en_US"))
			downloadLocale(customLocaleUrl, SimpleSettings.LOCALE, false);

		// Finally, override with user-defined keys
		final File localFile = FileUtil.getFile("lang/" + SimpleSettings.LOCALE + ".json");

		if (localFile.exists()) {
			final JsonObject localJson = readJsonFromFile(localFile);

			for (final String key : localJson.keySet())
				instance.dictionary.add(key, localJson.get(key));
		}
	}

	private static void downloadLocale(String baseUrl, String localeKey, boolean internalLocale) {
		JsonObject localeJson;

		if (baseUrl != null && baseUrl.endsWith("/"))
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

		if (baseUrl == null || offlineMode)
			try {
				localeJson = readJsonFromInternal("lang/" + localeKey + ".json");

			} catch (final IllegalArgumentException ex) {
				if (baseUrl != null)
					CommonCore.error(ex, "Embedded locale " + localeKey + " does not exist! Falling back to English.");

				return;
			}

		else
			try {
				localeJson = readJsonFromUrl(baseUrl + "/lang/" + localeKey + ".json");

			} catch (final IOException ex) {

				if (!internalLocale) {
					CommonCore.error(ex, "Locale " + localeKey + " does not exist! Falling back to English.");

					return;
				}

				if (ex instanceof UnknownHostException)
					CommonCore.warning("Unable to download locale " + localeKey + ", falling back to embedded.");

				else
					CommonCore.error(ex, "Unable to download locale " + localeKey);

				localeJson = readJsonFromInternal("lang/" + localeKey + ".json");
			}

		for (final String key : localeJson.keySet())
			instance.dictionary.add(key, localeJson.get(key));
	}

	/*
	 * Return all lines from the given external file as a json element
	 */
	private static JsonObject readJsonFromUrl(String url) throws IOException {
		final List<String> baseContent = FileUtil.readLinesFromUrl(url);

		return RemainCore.GSON.fromJson(baseContent.isEmpty() ? "{}" : String.join("\n", baseContent), JsonObject.class);
	}

	/*
	 * Return all lines from the given internal file as a json element
	 */
	private static JsonObject readJsonFromInternal(String path) {
		final List<String> baseContent = FileUtil.getInternalFileContent(path);

		if (baseContent == null)
			throw new IllegalArgumentException("Internal path with JSON file at '" + path + "' does not exist!");

		return RemainCore.GSON.fromJson(baseContent.isEmpty() ? "{}" : String.join("\n", baseContent), JsonObject.class);
	}

	/*
	 * Return all lines from the given internal file as a json element
	 */
	private static JsonObject readJsonFromFile(File file) {
		final List<String> baseContent = FileUtil.readLines(file);

		if (baseContent == null)
			throw new FoException("Internal path with JSON file at '" + file + "' does not exist!");

		return RemainCore.GSON.fromJson(baseContent.isEmpty() ? "{}" : String.join("\n", baseContent), JsonObject.class);
	}

	private JsonArray retrieveList(String path) {
		final JsonElement element = this.retrieve(path);

		if (element.isJsonArray())
			return element.getAsJsonArray();

		final JsonArray array = new JsonArray();

		array.add(element.getAsString());
		return array;
	}

	private JsonElement retrieve(String path) {
		ValidCore.checkBoolean(this.dictionary.has(path), "Missing localization key '" + path + "'");

		return this.dictionary.get(path);
	}

	private boolean has(String path) {
		return this.dictionary.has(path);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the given key for the given amount automatically
	 * singular or plural form including the amount
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String numberFormat(String path, long amount) {
		return CaseNumberFormat.fromString(plain(path)).formatWithCount(amount);
	}

	/**
	 * Return the given key for the given amount automatically
	 * singular or plural form excluding the amount
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String numberFormatNoAmount(String path, long amount) {
		return CaseNumberFormat.fromString(plain(path)).formatWithoutCount(amount);
	}

	/**
	 * Return a key from the localization file
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static String legacyVars(String path, Object... replacements) {
		final String value = legacy(path);

		return Variables.replace(value, null, CommonCore.newHashMap(replacements));
	}

	/**
	 * Return a key from the localization file
	 *
	 * @param path
	 * @return
	 */
	public static String legacy(String path) {
		return component(path).toLegacy();
	}

	/**
	 * Return a key from the localization file
	 *
	 * @param path
	 * @return
	 */
	public static String plain(String path) {
		return instance.retrieve(path).getAsString();
	}

	/**
	 * Return if the given key exists
	 *
	 * @param path
	 * @return
	 */
	public static boolean exists(String path) {
		return instance.has(path);
	}

	/**
	 * Return a key from the localization file
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static SimpleComponent componentVars(String path, Object... replacements) {
		final SimpleComponent component = component(path);

		return Variables.replace(component, null, CommonCore.newHashMap(replacements));
	}

	/**
	 * Return a key from the localization file
	 *
	 * @param path
	 * @return
	 */
	public static SimpleComponent component(String path) {
		return SimpleComponent.fromMini(plain(path));
	}

	/**
	 * Return an array from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @return
	 */
	public static SimpleComponent[] componentArray(String path) {
		return componentArrayVars(path);
	}

	/**
	 * Return an array from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static SimpleComponent[] componentArrayVars(String path, Object... replacements) {
		final List<SimpleComponent> components = new ArrayList<>();

		for (final JsonElement listElement : instance.retrieveList(path))
			components.add(Variables.replace(SimpleComponent.fromMini(listElement.getAsString()), null, CommonCore.newHashMap(replacements)));

		return components.toArray(new SimpleComponent[components.size()]);
	}

	/**
	 * Return an array from the localization file with {0} {1} etc. variables replaced.
	 *
	 * @param path
	 * @param replacements
	 * @return
	 */
	public static String[] legacyArrayVars(String path, Object... replacements) {
		final List<String> lines = new ArrayList<>();

		for (final JsonElement listElement : instance.retrieveList(path))
			lines.add(Variables.replace(listElement.getAsString(), null, CommonCore.newHashMap(replacements)));

		return CommonCore.toArray(lines);
	}

	/**
	 * The default keys from the main overlay.
	 */
	public static final class Default {

		/**
		 * The {date}, {date_short} and {date_month} formats.
		 */
		private static DateFormat dateFormat;
		private static DateFormat dateFormatShort;
		private static DateFormat dateFormatMonth;

		public static DateFormat getDateFormat() {
			if (dateFormat == null)
				dateFormat = makeFormat("format-date", "dd.MM.yyyy HH:mm:ss");

			return dateFormat;
		}

		public static DateFormat getDateFormatShort() {
			if (dateFormatShort == null)
				dateFormatShort = makeFormat("format-date-short", "dd.MM.yyyy HH:mm");

			return dateFormatShort;
		}

		/**
		 * The format used in the {timestamp} placeholder.
		 *
		 * @return
		 */
		public static DateFormat getDateFormatMonth() {
			if (dateFormatMonth == null)
				dateFormatMonth = makeFormat("format-date-month", "dd.MM HH:mm");

			return dateFormatMonth;
		}

		/*
		 * A helper method to create a date format from the given plain lang key.
		 */
		private static DateFormat makeFormat(String key, String def) {
			final String raw = exists(key) ? plain(key) : def;

			try {
				return new SimpleDateFormat(raw);

			} catch (final IllegalArgumentException ex) {
				CommonCore.throwError(ex, "Date format at '" + key + "' is invalid: '" + raw + "'! See https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for syntax'");

				return null;
			}
		}
	}
}
