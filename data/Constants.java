package Vorkath.data;

import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import simple.robot.utils.WorldArea;

public class Constants {

	public final static int START_ROCK_ID = 31990;
	public final static int VORKATH_SLEEPING_ID = 8059;
	public final static int VORKATH_WAKING_UP_ID = 8058;
	public final static int VORKATH_ID = 8061;
	public final static int VORKATH_RANGED_ANIM_ID = 1477;
	public final static int VORKATH_MAGE_ANIM_ID = 1479;
	public final static int VORKATH_FIREBALL_ANIM_ID = 7960;
	public final static int VORKATH_FIREBALL_PROJECTILE_ID = 1481;
	public final static int VORKATH_ZOMBIE_ATTACK_PROJECTILE_ID = 1484;
	public final static int VORKATH_ACID_ATTACK_PROJECTILE_ID = 1482;
	public final static int VORKATH_ACID_ATTACK_ANIM_ID = 100000;
	public static int ACID_TILE_ID = 32000;
	public static int[] ANTI_IDS = { 2446, 177, 175, 179, 12913, 12915, 12917, 12919 };
	public static int[] ANTIFIRE_IDS = { 2452, 2454, 2458, 2456 };
	public static int[] RANGE_POT__IDS = { 2444, 173, 171, 169 };
	public static int[] PRAYER_POT_IDS = { 2434, 139, 143, 141 };
	public static final int[] DEFENCE_POT_IDS = { ItemID.SUPER_DEFENCE1, ItemID.SUPER_DEFENCE3, ItemID.SUPER_DEFENCE4,
			ItemID.SUPER_DEFENCE2 };
	public final static WorldArea EDGEVILE_AREA = new WorldArea(new WorldPoint(3078, 3504, 0),
			new WorldPoint(3101, 3486, 0));
	public final static WorldArea VORKATH_START_AREA = new WorldArea(new WorldPoint(2270, 4054, 0),
			new WorldPoint(2276, 4035, 0));

}
