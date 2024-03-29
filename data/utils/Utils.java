package Vorkath.data.utils;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import Vorkath.data.Variables;
import simple.hooks.filters.SimpleEquipment.EquipmentSlot;
import simple.hooks.filters.SimplePrayers.Prayers;
import simple.hooks.filters.SimpleSkills.Skills;
import simple.hooks.simplebot.Game.Tab;
import simple.hooks.wrappers.SimpleItem;
import simple.hooks.wrappers.SimpleWidget;
import simple.robot.api.ClientContext;

public class Utils {

	private static String runescapeFormat(Integer number) {
		String[] suffix = { "K", "M", "B", "T" };
		int size = (number.intValue() != 0) ? (int) Math.log10(number.intValue()) : 0;
		if (size >= 3)
			while (size % 3 != 0)
				size--;
		return (size >= 3)
				? (String.valueOf(Math.round(number.intValue() / Math.pow(10.0D, size) * 10.0D) / 10.0D)
						+ suffix[size / 3 - 1])
				: (new StringBuilder(String.valueOf(number.intValue()))).toString();
	}

	public static Image getImage(String url) {
		try {
			return ImageIO.read(new URL(url));
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean isProjectileActive(int id) {
		if (ClientContext.instance().projectiles.getActiveProjectiles().size() > 0) {
			if (ClientContext.instance().projectiles.projectileActive(id)) {
				return true;
			}
		}
		return false;
	}

	public static void disablePrayer(Prayers prayer) {
		if (ClientContext.instance().prayers.prayerActive(prayer))
			ClientContext.instance().prayers.prayer(prayer, false);
	}

	public static void disableAllPrayers() {
		Stream.of(Prayers.values()).forEach(Utils::disablePrayer);
	}

	public static void enablePrayer(Prayers prayer) {
		if (ClientContext.instance().skills.level(Skills.PRAYER) <= 0)
			return;
		if (!ClientContext.instance().prayers.prayerActive(prayer)) {
			Variables.STATUS = "Enabling prayer";
			ClientContext.instance().prayers.prayer(prayer, true);
		}
	}

	public static String get(String url) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Scanner sc = new Scanner(new URL(url).openStream()); sc.hasNext();)
			sb.append(sc.nextLine()).append('\n');
		return sb.toString();
	}

	public static boolean inventoryContains(boolean all, String... itemName) {
		if (!all)
			return inventoryContains(itemName);
		return !ClientContext.instance().inventory.populate()
				.filter(p -> Stream.of(itemName).allMatch(arr -> p.getName().toLowerCase().contains(arr.toLowerCase())))
				.isEmpty();
	}

	public static boolean inventoryContains(String... itemName) {
		return !ClientContext.instance().inventory.populate()
				.filter(p -> Stream.of(itemName).anyMatch(arr -> p.getName().toLowerCase().contains(arr.toLowerCase())))
				.isEmpty();
	}

	public static SimpleItem getItem(String... itemName) {
		return ClientContext.instance().inventory.populate()
				.filter(p -> Stream.of(itemName).anyMatch(arr -> p.getName().toLowerCase().contains(arr.toLowerCase())))
				.next();
	}

	public static boolean isItemEquiped(String name) {
		if (ClientContext.instance().equipment.getEquippedItem(EquipmentSlot.WEAPON) != null
				&& ClientContext.instance().equipment.getEquippedItem(EquipmentSlot.WEAPON).getName().toLowerCase()
						.contains(name)) {
			return true;
		}

		return false;
	}

	public static int getPercentagePrayer() {
		float perc = ((float) ClientContext.instance().skills.level(Skills.PRAYER)
				/ ClientContext.instance().skills.realLevel(Skills.PRAYER));
		float perc1 = (perc * 100);
		return (int) perc1;
	}

	public static int getPercentageHitpoints() {
		// returns 50 for example
		float perc = ((float) ClientContext.instance().skills.level(Skills.HITPOINTS)
				/ ClientContext.instance().skills.realLevel(Skills.HITPOINTS));
		float perc1 = (perc * 100);
		return (int) perc1;
	}

	public static void openTab(Tab tab) {
		if (!isTabOpen(tab))
			ClientContext.instance().game.tab(tab);
	}

	public static boolean isTabOpen(Tab tab) {
		return ClientContext.instance().game.tab().equals(tab);
	}

	public static boolean doneDelay(long ms, int delay) {
		if (ms == 0)
			return true;
		long left = (ms + delay) - System.currentTimeMillis();
		return left <= 0;
	}

	public static String delayTimer(long ms, int delay) {
		long left = (ms + delay) - System.currentTimeMillis();
		if (left <= 0)
			return "";
		return String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toSeconds(left) / 60,
				TimeUnit.MILLISECONDS.toSeconds(left) % 60);
	}

	public static void drinkSinglePrayerPot(boolean check) {
		int percPrayerlvl = getPercentagePrayer();
		if (check && percPrayerlvl > 60)
			return;

		String[] restores = { "restore", "sanfew", "prayer" };
		SimpleItem restore = getItem(restores);

		if (restore != null && restore.click(0)) {
			Variables.STATUS = "Drinking prayer potion";
		}
	}

	public static void setZoom(int zoom) {
		ClientContext ctx = ClientContext.instance();
		ctx.viewport.pitch(100);
		ctx.viewport.angle(0);

		openTab(Tab.OPTIONS);

		SimpleWidget widget = ctx.widgets.getWidget(261, 8 + zoom);
		if (widget != null && widget.visibleOnScreen())
			widget.click(0);
	}

}