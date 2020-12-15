package Vorkath;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Vorkath.data.Supplies;
import Vorkath.data.Variables;
import Vorkath.data.Vorkath;
import Vorkath.data.utils.Utils;
import Vorkath.tasks.BankTask;
import Vorkath.tasks.NpcFighterTask;
import Vorkath.tasks.WalkerTask;
import net.runelite.api.VarPlayer;
import simple.hooks.scripts.Category;
import simple.hooks.scripts.ScriptManifest;
import simple.hooks.scripts.task.Task;
import simple.hooks.scripts.task.TaskScript;
import simple.hooks.simplebot.ChatMessage;
import simple.hooks.simplebot.Magic.SpellBook;
import simple.hooks.simplebot.teleporter.Teleporter;

@ScriptManifest(author = "Trester/Steganos", category = Category.COMBAT, description = "Does vorkath", name = "Zaros Vorkath", servers = {
		"Zaros" }, version = "0.1", discord = "")

public class Main extends TaskScript {
	private List<Task> tasks = new ArrayList<Task>();

	@Override
	public void paint(Graphics Graphs) {
		Graphics2D g = (Graphics2D) Graphs;

		g.setColor(Color.BLACK);
		g.fillRect(0, 230, 150, 55);
		g.setColor(Color.BLACK);
		g.drawRect(0, 230, 150, 55);
		g.setColor(Color.white);

		g.drawString("Private Vorkath v0.1", 7, 245);
		g.drawString("Uptime: " + Variables.START_TIME.toElapsedString(), 7, 257);
		g.drawString("Status: " + Variables.STATUS, 7, 269);
		g.drawString("Varbit: " + ctx.getClient().getVar(VarPlayer.IS_POISONED),7,280);


	}

	@Override
	public boolean prioritizeTasks() {
		return true;
	}

	@Override
	public List<Task> tasks() {
		return tasks;
	}

	@Override
	public void onChatMessage(ChatMessage msg) {

	}

	public boolean fightReady() {
		boolean hasAnti = false;
		boolean hasFood = false;
		boolean hasRunes = true;
		boolean hasAntiFire = true;

		return hasAnti && hasFood && hasRunes && hasAntiFire;
	}

	@Override
	public void onExecute() {
		try {
			doChecks();
			Utils.setZoom();
			startScript();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doChecks() {
		boolean stop = false;
		if (ctx.magic.spellBook() != SpellBook.MODERN) {
			ctx.log("Please switch to normal spellbook");
			stop = true;
		}
		if (!Utils.inventoryContains(true, "Chaos rune", "Earth rune", "Air rune")) {
			ctx.log("Please have required runes in inventory:\n%s, %s, %s", "Chaos rune", "Earth rune", "Air rune");
			// stop = true;
		}

		if (stop)
			ctx.stopScript();

	}

	@Override
	public void onTerminate() {
	}

	private void startScript() {
		Variables.vorkath = new Vorkath(ctx, this);
		Variables.supplies = new Supplies(ctx, this);
		Variables.teleporter = new Teleporter(ctx);

		Variables.bankTask = new BankTask(ctx, this);
		Variables.walkTask = new WalkerTask(ctx, this);
		Variables.fighterTask = new NpcFighterTask(ctx, this);

		tasks.addAll(Arrays.asList(Variables.bankTask, Variables.walkTask, Variables.fighterTask));
	}

	public void onMouse(MouseEvent e) {

	}
}
