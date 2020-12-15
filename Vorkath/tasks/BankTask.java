package Vorkath.tasks;

import Vorkath.Main;
import Vorkath.data.Variables;
import net.runelite.api.coords.WorldPoint;
import simple.hooks.filters.SimpleSkills.Skills;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;
import simple.robot.utils.WorldArea;

public class BankTask extends Task {
	private Main main;
	private WorldArea edge = new WorldArea(new WorldPoint(3073, 3516, 0), new WorldPoint(3108, 3471, 0));

	public BankTask(ClientContext ctx, Main main) {
		super(ctx);
		this.main = main;
	}

	@Override
	public boolean condition() {
		return Variables.supplies.shouldRestock();
	}

	@Override
	public void run() {

		if (!edge.containsPoint(ctx.players.getLocal().getLocation())) {
			ctx.magic.castSpellOnce("Home");
			ctx.sleep(250, 500);
			ctx.sleepCondition(() -> edge.containsPoint(ctx.players.getLocal().getLocation()), 1500);
			return;
		}
		if (ctx.skills.level(Skills.HITPOINTS) != ctx.skills.realLevel(Skills.HITPOINTS)
				|| ctx.skills.level(Skills.PRAYER) != ctx.skills.realLevel(Skills.PRAYER)) {

			SimpleObject box = ctx.objects.populate().filter(60003).nearest().next();
			if (box != null && box.validateInteractable())
				box.click("Heal");

			return;
		}

		SimpleObject bank = ctx.objects.populate().filter(10355).nearest().next();
		if (bank != null && bank.validateInteractable()) {
			bank.click("Last-preset");
			ctx.sleep(500, 1500);
		}

	}

	@Override
	public String status() {
		return "Restocking";
	}

}
