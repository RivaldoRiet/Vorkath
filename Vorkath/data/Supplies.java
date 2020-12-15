
package Vorkath.data;

import Vorkath.Main;
import Vorkath.data.utils.Timer;
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
		if (Variables.fighterTask.extremelyLowHealth()) return true;
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

	private static Timer antiFireTimer = new Timer(1);

	public void drinkAntiFire() {
		SimpleItem potion = ClientContext.instance().inventory.populate().filter(1).next();
		if (!antiFireTimer.isRunning() && potion != null) {
			potion.click("drink");
			antiFireTimer.setEndIn(300000);
			ClientContext.instance().sleep(150, 350);
		}
	}

	public void drinkAntiPoision() {
		SimpleItem anti = ctx.inventory.populate().filter(g -> g.getName().contains("Anti")).next();
		if (!antiFireTimer.isRunning() && anti != null) {
			anti.click(0);
			antiFireTimer.setEndIn(300000);
			ClientContext.instance().sleep(150, 350);
		}
	}



	public void eatFood() {
		SimpleItem food = ctx.inventory.populate().filterHasAction("Eat").next();

		if (food != null) food.click(0);
	}

}