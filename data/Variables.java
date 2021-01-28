
package Vorkath.data;

import Vorkath.data.utils.Timer;
import Vorkath.tasks.BankTask;
import Vorkath.tasks.NpcFighterTask;
import Vorkath.tasks.RecollectTask;
import Vorkath.tasks.WalkerTask;
import simple.hooks.simplebot.teleporter.Teleporter;

public class Variables {

	public static boolean RECOLLECT_ITEMS = false;
	public static boolean FORCE_BANK = false;

	public static String STATUS = "Booting up";
	public static Timer START_TIME = new Timer();

	public static BankTask bankTask;
	public static WalkerTask walkTask;
	public static NpcFighterTask fighterTask;
	public static RecollectTask recollectTask;

	public static Vorkath vorkath;
	public static Teleporter teleporter;
	public static Supplies supplies;
}