package Vorkath;

import java.awt.*;
import java.net.URL;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import net.runelite.api.coords.WorldPoint;
import simple.hooks.scripts.Category;
import simple.hooks.scripts.ScriptManifest;
import simple.hooks.scripts.task.Task;
import simple.hooks.scripts.task.TaskScript;
import simple.hooks.simplebot.ChatMessage;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.utils.WorldArea;

@ScriptManifest(author = "Trester/Steganos", category = Category.COMBAT, description = "Does vorkath", name = "Zaros Vorkath Slayer", servers = {
		"Zaros" }, version = "0.1", discord = "")

public class Main extends TaskScript {
	private long STARTTIME, UPTIME;
	private List<Task> tasks = new ArrayList<Task>();
	public String status = "";
	private WorldArea edge = new WorldArea(new WorldPoint(3073, 3516, 0), new WorldPoint(3108, 3471, 0));

	@Override
	public void paint(Graphics Graphs) {
		Graphics2D g = (Graphics2D) Graphs;

		g.setColor(Color.BLACK);
		g.fillRect(0, 230, 150, 55);
		g.setColor(Color.BLACK);
		g.drawRect(0, 230, 150, 55);
		g.setColor(Color.white);
		
		g.drawString("Private Vorkath v0.1", 7, 245);
		g.drawString("Uptime: " + this.ctx.paint.formatTime(this.UPTIME), 7, 257);
		g.drawString("Status: " + this.status, 7, 269);

	}

	@Override
	public boolean prioritizeTasks() {
		return false;
	}

	@Override
	public List<Task> tasks() {
		return tasks;
	}
	
	public boolean shouldRestock() {

		if (!Utils.inventoryContains("shark", "anglerfish", "manta ray")) {
			status = ("Restocking due to no food");
			return true;
		}
		
		if (!Utils.inventoryContains("sanfew", "restore")) {
			status = ("Restocking due to no restores");
			return true;
		}
		
		return false;
	}

	@Override
	public void onChatMessage(ChatMessage msg) {

	}

	@Override
	public void onExecute() {
	}

	@Override
	public void onTerminate() {
	}

	public boolean inVorkathArea()
	{
		SimpleObject rock = ctx.objects.populate().filter(Variables.startRockId).next();
		if (rock != null) {
			if (rock.getLocation().getY() < ctx.players.getLocal().getLocation().getY()) {
				// player is above rock
				return true;
			}
		}
		return false;
	}
	
	private boolean containsItem(String itemName) {
		return !ctx.inventory.populate().filter(p -> p.getName().toLowerCase().contains(itemName.toLowerCase()))
				.isEmpty();
	}

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

	private void startScript() {
		tasks.addAll(Arrays.asList(new BankTask(ctx, this),
				new WalkerTask(ctx, this), new NpcFighterTask(ctx, this)));
	}

	public static String get(String url) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Scanner sc = new Scanner(new URL(url).openStream()); sc.hasNext();)
			sb.append(sc.nextLine()).append('\n');
		return sb.toString();
	}

	public void onMouse(MouseEvent e) {

	}
}
