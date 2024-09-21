package org.mineacademy.fo.model;

import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.platform.FoundationPlayer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a simple title message
 */
@RequiredArgsConstructor
public final class TitleMessage implements ConfigSerializable {

	/**
	 * title
	 */
	@Getter
	private final String titleMessage;

	/**
	 * title
	 */
	@Getter
	private final String subtitleMessage;

	/**
	 * fadeIn
	 */
	private final int fadeIn;

	/**
	 * stay
	 */
	private final int stay;

	/**
	 * stay
	 */
	private final int fadeOut;

	/**
	 * Displays this title message to the given player
	 *
	 * @param sender
	 */
	public void displayTo(FoundationPlayer sender) {
		sender.sendTitle(this.fadeIn, this.stay, this.fadeOut, this.titleMessage, this.subtitleMessage);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.titleMessage + " " + this.subtitleMessage + " " + this.fadeIn + " " + this.stay + " " + this.fadeOut;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"title", this.titleMessage,
				"subtitle", this.subtitleMessage,
				"fadeIn", this.fadeIn,
				"stay", this.stay,
				"fadeOut", this.fadeOut);
	}

	/**
	 * Create a new title message from the given map
	 *
	 * @param map
	 * @return
	 */
	public static TitleMessage deserialize(SerializedMap map) {
		final String title = map.getString("title");
		final String subtitle = map.getString("subtitle");
		final int fadeIn = map.getInteger("fadeIn");
		final int stay = map.getInteger("stay");
		final int fadeOut = map.getInteger("fadeOut");

		return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
	}
}