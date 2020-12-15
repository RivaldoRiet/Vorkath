package Vorkath.data;

import Vorkath.Main;
import Vorkath.data.utils.Utils;
import net.runelite.api.coords.WorldPoint;
import simple.hooks.wrappers.SimpleNpc;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;
import simple.robot.utils.WorldArea;

public class Vorkath {

	private ClientContext ctx;
	private Main main;

	public Vorkath(ClientContext ctx, Main main) {
		this.ctx = ctx;
		this.main = main;
	}

	public SimpleNpc get() {
		return get(false);
	}

	public SimpleNpc get(boolean sleeping) {
		return ctx.npcs.populate().filter(sleeping ? Constants.VORKATH_SLEEPING_ID : Constants.VORKATH_ID).next();
	}

	public boolean isActive() {
		return get() != null;
	}

	public boolean isSleeping() {
		return get(true) != null;
	}

	public boolean isFireBallActive() {
		return Utils.isProjectileActive(Constants.VORKATH_FIREBALL_PROJECTILE_ID);
	}

	public boolean inInstance() {
		SimpleObject rock = ctx.objects.populate().filter(Constants.START_ROCK_ID).next();
		return rock != null && rock.getLocation().getY() < ctx.players.getLocal().getLocation().getY();
	}

	public int getVorkathHealth()
	{
		SimpleNpc vorkath = get(false);
		if (vorkath != null && vorkath.getHealthRatio() != -1) {
			return vorkath.getHealthRatio();
		}
		return -1;
	}

	public WorldArea getArea() {
		WorldArea w = null;
		if (inInstance()) {
			SimpleObject rock = ctx.objects.populate().filter(Constants.START_ROCK_ID).next();
			if (rock != null) {
				WorldPoint southWest = new WorldPoint(rock.getLocation().getX() - 10, rock.getLocation().getY() + 1, 0);
				WorldPoint northEast = new WorldPoint(rock.getLocation().getX() + 10, rock.getLocation().getY() + 24,
						0);
				w = new WorldArea(southWest, northEast);
			}
		}
		return w;
	}

	public WorldArea rangeAttackArea() {
		WorldArea w = null;
		if (inInstance()) {
			SimpleObject rock = ctx.objects.populate().filter(Constants.START_ROCK_ID).next();
			if (rock != null) {
				WorldPoint southWest = new WorldPoint(rock.getLocation().getX() - 2, rock.getLocation().getY() + 3, 0);
				WorldPoint northEast = new WorldPoint(rock.getLocation().getX() + 2, rock.getLocation().getY() + 6,
						0);
				w = new WorldArea(southWest, northEast);
			}
		}
		return w;
	}
}