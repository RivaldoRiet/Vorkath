
package Vorkath.data;

import Vorkath.Main;
import Vorkath.data.utils.Timer;
import Vorkath.data.utils.Utils;
import simple.hooks.filters.SimpleSkills;
import simple.hooks.filters.SimpleSkills.Skills;
import simple.hooks.wrappers.SimpleItem;
import simple.robot.api.ClientContext;

public class Supplies {

	private ClientContext ctx;
	private Main main;

	public Supplies(ClientContext ctx, Main main) {
		this.ctx = ctx;
		this.main = main;
	}

	public boolean shouldRestock() {
		if (extremelyLowHealth()
				|| (quiteLowHealth() && ctx.inventory.populate().filterHasAction("Eat").population() == 0)) {
			return true;
		}
		if (Variables.fighterTask.extremelyLowHealth())
			return true;
		
		//if (!Utils.inventoryContains("shark", "anglerfish", "manta ray", "pizza", "prayer potion"))
		//	return true;
		return false;
		/*
		 * if (!Utils.inventoryContains("shark", "anglerfish", "manta ray")) { status =
		 * ("Restocking due to no food"); return true; }
		 *
		 * if (!Utils.inventoryContains("sanfew", "restore")) { status =
		 * ("Restocking due to no restores"); return true; }
		 *
		 * return false;
		 */
	}

	private boolean fullInventWithBones() {
		if (ctx.inventory.populate().population() == 28 && Utils.inventoryContains("Dragon bones")) {
			return true;
		}
		return false;
	}
	
	public boolean extremelyLowHealth() {
		return getPercentageHitpoints() < 20;
	}

	public boolean quiteLowHealth() {
		return getPercentageHitpoints() < 40;
	}
	
	private static Timer antiFireTimer = new Timer(1);

	public void drinkAntiFire() {
		SimpleItem potion = ClientContext.instance().inventory.populate().filter(Constants.ANTIFIRE_IDS).next();
		if (!antiFireTimer.isRunning() && potion != null) {
			potion.click("drink");
			antiFireTimer.setEndIn(200000);
			ClientContext.instance().sleep(150, 350);
		}
	}

	public boolean lowHealth() {
		return getPercentageHitpoints() <= 77;
	}
	
	public void drinkAntiPoison() {
		if (lowHealth()) {
			Variables.supplies.eatFood();
		}
		SimpleItem potion = ClientContext.instance().inventory.populate().filter(Constants.ANTI_IDS).next();
		if (potion != null) {
			Variables.STATUS = "Drinking antipoison";
			potion.click(0);
		}

	}

	public int getPercentageHitpoints() {
		// returns 50 for example
		float perc = ((float) ClientContext.instance().skills.level(Skills.HITPOINTS)
				/ ClientContext.instance().skills.realLevel(Skills.HITPOINTS));
		float perc1 = (perc * 100);
		return (int) perc1;
	}
	
	public void eatFood() {
		SimpleItem food = ctx.inventory.populate().filterHasAction("Eat").next();

		if (food != null) { 
			Variables.STATUS = "Clicking food";
			food.click(0);
		}
	}

	public void drinkRangedPotion() {
		SimpleItem rangePot = ctx.inventory.populate().filter(Constants.RANGE_POT__IDS).next();
		if (rangePot != null) {
			Variables.STATUS = "Clicking range potion";
			rangePot.click(0);
		}
	}

	public boolean shouldDrinkRangePot() {
		int max = ctx.skills.realLevel(SimpleSkills.Skills.RANGED);
		return max + 8 > ctx.skills.level(SimpleSkills.Skills.RANGED);
	}

	public void drinkDefencePotion() {
		SimpleItem pot = ctx.inventory.populate().filter(Constants.DEFENCE_POT_IDS).next();
		if (pot != null) {
			Variables.STATUS = "Clicking defense potion";
			pot.click(0);
		}
	}

	public boolean shouldDrinkDefencePot() {
		int max = ctx.skills.realLevel(SimpleSkills.Skills.DEFENCE);
		return max + 16 > ctx.skills.level(SimpleSkills.Skills.DEFENCE);
	}
}