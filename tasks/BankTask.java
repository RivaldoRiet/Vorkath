package Vorkath.tasks;

import Vorkath.Main;
import Vorkath.data.Constants;
import Vorkath.data.Variables;
import Vorkath.data.utils.Utils;
import net.runelite.api.VarPlayer;
import simple.hooks.filters.SimpleSkills.Skills;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;

public class BankTask extends Task {
	private Main main;

	public BankTask(ClientContext ctx, Main main) {
		super(ctx);
		this.main = main;
	}

	@Override
	public boolean condition() {
		return !Variables.RECOLLECT_ITEMS && ctx.pathing.inArea(Constants.EDGEVILE_AREA)
				|| !Variables.RECOLLECT_ITEMS && Variables.supplies.shouldRestock();
	}

	@Override
	public void run() {

		if (!ctx.pathing.inArea(Constants.EDGEVILE_AREA)) {
			Variables.STATUS = "Teleporting to edgeville";
		}
		ctx.pathing.running(true);

		Utils.disableAllPrayers();
		//if (Variables.RECOLLECT_ITEMS) {
			//ctx.stopScript();
			//return;
		//}
		
		SimpleObject bank = ctx.objects.populate().filter(10355).nearest().next();
		if (ctx.skills.level(Skills.HITPOINTS) < ctx.skills.realLevel(Skills.HITPOINTS)
				|| ctx.skills.level(Skills.PRAYER) != ctx.skills.realLevel(Skills.PRAYER)) {
			Variables.STATUS = "Healing to full";
			SimpleObject box = ctx.objects.populate().filter(60003).nearest().next();
			if (box != null && box.validateInteractable())
				box.click("Heal");
		} else if (shouldBank() && bank != null && bank.validateInteractable()) {
			Variables.STATUS = "Getting last preset";
			bank.click("Last-preset");
			ctx.sleepCondition(() -> ctx.inventory.inventoryFull(), 1500);
			if (isPoisonedOrVenomed())
				Variables.supplies.drinkAntiPoison();
			return;
		} else if (Variables.teleporter.open()) {
			if (isPoisonedOrVenomed()) {
				Variables.supplies.drinkAntiPoison();
			} else {
				Variables.STATUS = "Teleporting to vorkath";
				Variables.teleporter.teleportStringPath("Bossing", "Vorkath");
				ctx.sleep(150, 400);
				ctx.sleepCondition(
						() -> Constants.VORKATH_START_AREA.containsPoint(ctx.players.getLocal().getLocation()), 1500);
			}
		}

	}

	public boolean isPoisonedOrVenomed() {
		return ctx.getClient().getVar(VarPlayer.IS_POISONED) >= 30;
	}

	private boolean shouldBank() {
		if (!ctx.inventory.inventoryFull()) {
			return true;
		}
		if (Utils.inventoryContains("dragon bones")) {
			return true;
		}

		return false;
	}

	@Override
	public String status() {
		return "Restocking";
	}

}
