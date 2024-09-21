package org.mineacademy.fo.exception;

import org.mineacademy.fo.MessengerCore;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.remain.CompChatColor;

/**
 * Represents a silent exception with a localizable message.
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The messages to send to the command sender, null if not set
	 */
	private final SimpleComponent[] components;

	/**
	 * Create a new command exception
	 */
	public CommandException() {
		this((SimpleComponent[]) null);
	}

	/**
	 * Create a new command exception with message for the command sender
	 *
	 * @param components
	 */
	public CommandException(SimpleComponent... components) {
		super("");

		this.components = components;
	}

	public final SimpleComponent[] getComponents() {
		return this.components == null ? new SimpleComponent[0] : this.components;
	}

	public final void sendErrorMessage(FoundationPlayer player) {
		if (this.components != null)
			if (this.components.length == 1)
				MessengerCore.error(player, this.components[0]);
			else
				for (final SimpleComponent component : this.components)
					component.color(CompChatColor.RED).send(player);
	}

	@Override
	public final String getMessage() {
		return this.components != null ? SimpleComponent.fromChildren(this.components).toLegacy() : "";
	}
}
