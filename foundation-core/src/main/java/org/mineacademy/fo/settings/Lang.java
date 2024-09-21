package org.mineacademy.fo.settings;

import java.io.File;
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
import org.mineacademy.fo.model.CaseNumberFormat;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.RemainCore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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

	public static final class Storage {

		public static File dump() {
			final File localFile = FileUtil.createIfNotExists("lang/" + SimpleSettings.LOCALE + ".json");
			final JsonObject localJson = RemainCore.GSON.fromJson(String.join("\n", FileUtil.readLinesFromFile(localFile)), JsonObject.class);

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

		public static void download() {
			final String englishLangTag = Locale.US.getLanguage() + "_" + Locale.US.getCountry();
			final boolean isEnglish = SimpleSettings.LOCALE.equals("en_US");

			List<String> content;

			// Foundation locale
			{
				content = FileUtil.readLinesFromInternalPath("lang/overlay/" + englishLangTag + ".json");

				// Base overlay must be set
				ValidCore.checkNotNull(content, "Locale file lang/overlay/en_US.json is missing! Did you reload or used PlugMan(X)? Make sure Foundation is shaded properly!");
				putToDictionary(content);

				// Language specific base overlay can be null
				if (!isEnglish) {
					content = FileUtil.readLinesFromInternalPath("lang/overlay/" + SimpleSettings.LOCALE + ".json");

					putToDictionary(content);
				}

			}

			// Plugin-specific
			{
				// Optional
				content = FileUtil.readLinesFromInternalPath("lang/" + englishLangTag + ".json");
				putToDictionary(content);

				if (!isEnglish) {

					// Base overlay must be set when using non-English locale
					ValidCore.checkNotNull(content, "When using non-English locale (" + SimpleSettings.LOCALE + "), the base overlay en_US.json must exists in " + Platform.getPlugin().getName());

					content = FileUtil.readLinesFromInternalPath("lang/" + SimpleSettings.LOCALE + ".json");
					putToDictionary(content);
				}
			}

			// On disk
			{
				// Start with base locale as overlay
				content = FileUtil.readLinesFromFile("lang/" + englishLangTag + ".json");
				putToDictionary(content);

				if (!isEnglish) {
					content = FileUtil.readLinesFromFile("lang/" + SimpleSettings.LOCALE + ".json");
					putToDictionary(content);
				}
			}
		}

		private static void putToDictionary(List<String> content) {
			if (content != null && !content.isEmpty()) {
				final JsonObject json = RemainCore.GSON.fromJson(String.join("\n", content), JsonObject.class);

				for (final String key : json.keySet())
					instance.dictionary.add(key, json.get(key));
			}
		}
	}
}
