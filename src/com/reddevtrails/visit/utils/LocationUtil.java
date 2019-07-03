package com.reddevtrails.visit.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.reddevtrails.visit.Settings;

public class LocationUtil {
	// The player can stand inside these materials
	private static final Set<Material> HOLLOW_MATERIALS = new HashSet<>();
	private static final Set<Material> TRANSPARENT_MATERIALS = new HashSet<>();
	private static final Set<Material> BEDS = new HashSet<>(Arrays.asList(Material.WHITE_BED, Material.ORANGE_BED,
			Material.MAGENTA_BED, Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED, Material.PINK_BED,
			Material.GRAY_BED, Material.LIGHT_GRAY_BED, Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
			Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED, Material.BLACK_BED));

	static {
		// TODO: Look into non-deprecated or safer detection method
		for (Material mat : Material.values()) {
			if (mat.isTransparent()) {
				HOLLOW_MATERIALS.add(mat);
			}
		}

		TRANSPARENT_MATERIALS.addAll(HOLLOW_MATERIALS);
		TRANSPARENT_MATERIALS.add(Material.WATER);
	}

	public static final int RADIUS = 3;
	public static final Vector3D[] VOLUME;

	public static ItemStack convertBlockToItem(final Block block) {
		return new ItemStack(block.getType(), 1);
	}

	public static class Vector3D {
		public int x;
		public int y;
		public int z;

		Vector3D(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	static {
		List<Vector3D> pos = new ArrayList<>();
		for (int x = -RADIUS; x <= RADIUS; x++) {
			for (int y = -RADIUS; y <= RADIUS; y++) {
				for (int z = -RADIUS; z <= RADIUS; z++) {
					pos.add(new Vector3D(x, y, z));
				}
			}
		}
		pos.sort(Comparator.comparingInt(a -> (a.x * a.x + a.y * a.y + a.z * a.z)));
		VOLUME = pos.toArray(new Vector3D[0]);
	}
	
	public static boolean blockIsBed(Material bed) {
		return BEDS.contains(bed);
	}

	public static boolean blockIsAboveAir(final World world, final int x, final int y, final int z) {
		return y > world.getMaxHeight() || HOLLOW_MATERIALS.contains(world.getBlockAt(x, y - 1, z).getType());
	}
	
	public static boolean blockIsUnsafeForUser(final Player player, final Location loc) {
		return blockIsUnsafeForUser(player, loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public static boolean blockIsUnsafeForUser(final Player player, final World world, final int x, final int y, final int z) {
		if (player.isOnline() && (player.getGameMode() == GameMode.CREATIVE || !Settings.safeTeleport)) {
			return false;
		}

		if (blockIsDamaging(world, x, y, z)) {
			return true;
		}
		
		return blockIsAboveAir(world, x, y, z);
	}

	public static boolean blockIsUnsafe(final World world, final int x, final int y, final int z) {
		return blockIsDamaging(world, x, y, z) || blockIsAboveAir(world, x, y, z);
	}

	public static boolean blockIsDamaging(final World world, final int x, final int y, final int z) {
		final Block below = world.getBlockAt(x, y - 1, z);

		List<Material> harmfulMaterials = Arrays.asList(Material.LAVA, Material.FIRE);

		if (harmfulMaterials.contains(below.getType()))
			return true;

		if (blockIsBed(below.getType()))
			return true;

		if (world.getBlockAt(x, y, z).getType() == Material.NETHER_PORTAL) {
			return true;
		}

		return (!HOLLOW_MATERIALS.contains(world.getBlockAt(x, y, z).getType())) || (!HOLLOW_MATERIALS.contains(world.getBlockAt(x, y + 1, z).getType()));
	}

	// Not needed if using getSafeDestination(loc)
	public static Location getRoundedDestination(final Location loc) {
		final World world = loc.getWorld();
		int x = loc.getBlockX();
		int y = (int) Math.round(loc.getY());
		int z = loc.getBlockZ();
		float yaw = loc.getYaw();
		float pitch = loc.getPitch();
		
		// Check if we need to do any tweaking to pitch and yaw
		int rounding = Settings.cardinalRounding;
		if (rounding != 0)
			yaw = Math.round(yaw / rounding) * rounding;
		if (Settings.centerUpDown)
			pitch = 0;
		
		return new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
	}

	public static Location getSafeDestination(final Player player, final Location loc) throws Exception {
		if (player.isOnline() && (player.getGameMode() == GameMode.CREATIVE || !Settings.safeTeleport)) {
			if (player.getAllowFlight() && shouldFly(loc)) {
				player.setFlying(true);
			}
			return getRoundedDestination(loc);
		}
		return getSafeDestination(loc);
	}

	public static Location getSafeDestination(final Location loc) throws Exception {
		if (loc == null || loc.getWorld() == null) {
			throw new Exception("destinationNotSet");
		}
		final World world = loc.getWorld();
		int x = loc.getBlockX();
		int y = (int) Math.round(loc.getY());
		int z = loc.getBlockZ();
		final int origX = x;
		final int origY = y;
		final int origZ = z;
		// try down
		while (blockIsAboveAir(world, x, y, z)) {
			y -= 1;
			if (y < 0) {
				y = origY;
				break;
			}
		}
		// try up
		while (blockIsAboveAir(world, x, y, z)) {
			y += 1;
			if (y >= world.getMaxHeight()) {
				y = origY;
				break;
			}
		}
		if (blockIsUnsafe(world, x, y, z)) {
			x = Math.round(loc.getX()) == origX ? x - 1 : x + 1;
			z = Math.round(loc.getZ()) == origZ ? z - 1 : z + 1;
		}
		int i = 0;
		while (blockIsUnsafe(world, x, y, z)) {
			i++;
			if (i >= VOLUME.length) {
				x = origX;
				y = origY + RADIUS;
				z = origZ;
				break;
			}
			x = origX + VOLUME[i].x;
			y = origY + VOLUME[i].y;
			z = origZ + VOLUME[i].z;
		}
		while (blockIsUnsafe(world, x, y, z)) {
			y += 1;
			if (y >= world.getMaxHeight()) {
				x += 1;
				break;
			}
		}
		while (blockIsUnsafe(world, x, y, z)) {
			y -= 1;
			if (y <= 1) {
				x += 1;
				y = world.getHighestBlockYAt(x, z);
				if (x - 48 > loc.getBlockX()) {
					throw new Exception("holeInFloor");
				}
			}
		}
		return getRoundedDestination(new Location(world, x, y, z, loc.getYaw(), loc.getPitch()));
	}

	public static boolean shouldFly(Location loc) {
		final World world = loc.getWorld();
		final int x = loc.getBlockX();
		int y = (int) Math.round(loc.getY());
		final int z = loc.getBlockZ();
		int count = 0;
		while (LocationUtil.blockIsUnsafe(world, x, y, z) && y > -1) {
			y--;
			count++;
			if (count > 2) {
				return true;
			}
		}

		return y < 0;
	}
}
