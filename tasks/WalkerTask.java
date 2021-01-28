package Vorkath.tasks;

import Vorkath.Main;
import Vorkath.data.Constants;
import Vorkath.data.Variables;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;

public class WalkerTask extends Task {

	private Main main;

	public WalkerTask(ClientContext ctx, Main main) {
		super(ctx);
		this.main = main;
	}

	@Override
	public boolean condition() {
		return !Variables.RECOLLECT_ITEMS && !Variables.vorkath.inInstance() && ctx.players.getLocal().getHealth() >= 80 && ctx.inventory.populate().population() == 28;
	}

	@Override
	public void run() {
		Variables.STATUS = "Walker task";

		if (!Constants.VORKATH_START_AREA.containsPoint(ctx.players.getLocal().getLocation())) {

			if (Variables.teleporter.open()) {
				Variables.teleporter.teleportStringPath("Bossing", "Vorkath");
				ctx.sleep(150, 400);
				ctx.sleepCondition(
						() -> Constants.VORKATH_START_AREA.containsPoint(ctx.players.getLocal().getLocation()), 1500);
			}
		} else {

			SimpleObject rock = ctx.objects.populate().filter(Constants.START_ROCK_ID).next();
			if (rock != null) {
				rock.turnTo();
				if (rock.validateInteractable())
					rock.click("Climb-over");
				ctx.sleepCondition(() -> Variables.vorkath.inInstance(), 5500);
			}

		}

	}

	@Override
	public String status() {
		return "Walking to vorkath";
	}

}
