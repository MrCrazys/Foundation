package org.mineacademy.fo.command;

import java.util.List;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.settings.Lang;

/**
 * A ready to use sub command enabling users to send conversation
 * input in case other plugins are blocking it and we are conversing
 * with the sender.
 *
 * A conversation means that we are waiting for player to type something
 * into the chat to process it. Such as the Boss plugin asks to type a Boss
 * name the player wants to create.
 */
public final class ConversationCommand extends SimpleSubCommand {

	public ConversationCommand() {
		super("conversation");

		this.setDescription("Reply to a server's conversation manually.");
		this.setUsage("<message ...>");
		this.setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		this.checkConsole();
		this.checkBoolean(this.getPlayer().isConversing(), Lang.component("conversation-not-conversing"));

		this.getPlayer().acceptConversationInput(CommonCore.joinRange(0, this.args));
	}

	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}