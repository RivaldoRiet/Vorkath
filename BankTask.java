package Vorkath;

import net.runelite.api.coords.WorldPoint;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleItem;
import simple.hooks.wrappers.SimpleWidget;
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
		return main.shouldRestock();
	}

	@Override
	public void run() {
		
	}
	
	@Override
	public String status() {
		return "Restocking";
	}
	
}
