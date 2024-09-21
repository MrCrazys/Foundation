package org.mineacademy.fo.command;

import java.io.File;
import java.util.List;

import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.SimpleSettings;

public final class DumpLocaleCommand extends SimpleSubCommandCore {

	public DumpLocaleCommand() {
		super("dumplocale|dumploc");

		this.setMaxArguments(0);

		// TODO too long description in command label on 1.8.8
		this.setDescription("Copy language file to lang/ folder so you can edit it. This uses 'Locale' key from settings.yml. Existing file will be updated with new keys and unused ones will be deleted.");
	}

	@Override
	protected void onCommand() {
		tellInfo("Dumping or updating " + SimpleSettings.LOCALE + " locale file...");

		final File dumped = Lang.Storage.dump();
		final File rootFile = Platform.getPlugin().getDataFolder();

		tellSuccess("Locale file dumped to " + dumped.getAbsolutePath().replace(rootFile.getParentFile().getAbsolutePath(), "") + ". Existing keys were updated, see console for details.");
	}

	/**
	 * @see org.mineacademy.fo.command.SimpleCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {

		if (args.length == 1)
			return completeLastWord("en_US");

		return NO_COMPLETE;
	}
}
