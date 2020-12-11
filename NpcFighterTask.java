package Vorkath;

import simple.hooks.scripts.task.Task;
import simple.robot.api.ClientContext;

public class NpcFighterTask extends Task {
	private Main main;

	public NpcFighterTask(ClientContext ctx, Main main) {
		super(ctx);
		this.main = main;
	}

	@Override
	public boolean condition() {
		return !main.shouldRestock() && main.inVorkathArea();
	}

	@Override
	public void run() {
      
	}
	
	@Override
	public String status() {
		return "Fighting vorkath";
	}
	
}
