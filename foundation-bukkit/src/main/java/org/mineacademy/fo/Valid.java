package org.mineacademy.fo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Valid extends ValidCore {

	/**
	 * Check if the player has the given permission, if false we send him a no permissions
	 * message and return false, otherwise no message is sent and we return true
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean checkPermission(final CommandSender sender, final String permission) {
		if (!sender.hasPermission(permission)) {
			Lang.componentVars("no-permission", "permission", permission).send(Platform.toPlayer(sender));

			return false;
		}

		return true;
	}

	/**
	 * Check if the code calling this method is run from the main thread,
	 * failing with the error message if otherwise
	 *
	 * @param asyncErrorMessage
	 */
	public static void checkSync(final String asyncErrorMessage) {
		Valid.checkBoolean(Bukkit.isPrimaryThread(), asyncErrorMessage);
	}

	/**
	 * Check if the code calling this method is run from a different than main thread,
	 * failing with the error message if otherwise
	 *
	 * @param syncErrorMessage
	 */
	public static void checkAsync(final String syncErrorMessage) {
		Valid.checkBoolean(!Bukkit.isPrimaryThread() || Remain.isFolia(), syncErrorMessage);
	}

	/**
	 * Return true if all x-y-z coordinates of the given vector are finite valid numbers
	 * (see {@link Double#isFinite(double)})
	 *
	 * @param vector
	 * @return
	 */
	public static boolean isFinite(final Vector vector) {
		return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Equality checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the two locations has same world and block positions
	 *
	 * @param first
	 * @param sec
	 * @return
	 */
	public static boolean locationEquals(final Location first, final Location sec) {

		if (first == null && sec == null)
			return true;

		if ((first == null && sec == null) || (first != null && sec == null))
			return false;

		try {
			if (!first.getWorld().getName().equals(sec.getWorld().getName()))
				return false;
		} catch (final NullPointerException ex) {
			// Ignore
		}

		return first.getBlockX() == sec.getBlockX() && first.getBlockY() == sec.getBlockY() && first.getBlockZ() == sec.getBlockZ();
	}
}
