package org.mineacademy.fo.remain.nbt;

import org.bukkit.Chunk;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ValidCore;

public class NBTChunk {

	private final Chunk chunk;

	public NBTChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.16.4+!
	 *
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	public NBTCompound getPersistentDataContainer() {
		ValidCore.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_16), "Chunk#getPersistentDataContainer() is only available in 1.16+");

		return new NBTPersistentDataContainer(this.chunk.getPersistentDataContainer());
	}

}
