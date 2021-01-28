package Vorkath.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import Vorkath.Main;
import Vorkath.data.utils.Utils;
import net.runelite.api.coords.WorldPoint;
import simple.hooks.queries.SimpleEntityQuery;
import simple.hooks.wrappers.SimpleNpc;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;
import simple.robot.utils.WorldArea;

public class Vorkath {

	private ClientContext ctx;
	private Main main;
	public WorldPoint debugTile = null;
	public long lastFireBallMove = 0;

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

	public boolean isSpawnActive() {
		return Utils.isProjectileActive(7960);
	}
	
	public boolean isZombieSpawnActive() {
		return Utils.isProjectileActive(Constants.VORKATH_ZOMBIE_ATTACK_PROJECTILE_ID);
	}

	public boolean isFireBallActive() {
		return Utils.isProjectileActive(Constants.VORKATH_FIREBALL_PROJECTILE_ID);
	}

	public boolean inInstance() {
		SimpleObject rock = ctx.objects.populate().filter(Constants.START_ROCK_ID).next();
		return rock != null && rock.getLocation().getY() < ctx.players.getLocal().getLocation().getY();
	}

	public int getVorkathHealth() {
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
				WorldPoint southWest = new WorldPoint(rock.getLocation().getX() - 1, rock.getLocation().getY() + 2, 0);
				WorldPoint northEast = new WorldPoint(rock.getLocation().getX() + 2, rock.getLocation().getY() + 3, 0);
				w = new WorldArea(southWest, northEast);
			}
		}
		return w;
	}

	private boolean isAcidLanding() {
		return ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() > 0 || acidLanding();
	}

	private boolean acidLanding() {
		return ctx.projectiles.projectileActive(1483);
	}

	public WorldPoint getStartingPointAcidSingle() {
		if (!isAcidLanding()) {
			return null;
		}

		WorldArea a = Variables.vorkath.getArea();

		for (WorldPoint w : a.getWorldPoints()) {
			if (w.distanceTo(ctx.players.getLocal().getLocation()) >= 8) {
				continue;
			}

			if (containsAcid(w)) {
				continue;
			}

			WorldPoint east1 = new WorldPoint(w.getX() + 1, w.getY(), 0);
			if (containsAcid(east1)) {
				continue;
			}

			WorldPoint east2 = new WorldPoint(w.getX() + 2, w.getY(), 0);
			if (containsAcid(east2)) {
				continue;
			}

			WorldPoint east3 = new WorldPoint(w.getX() + 3, w.getY(), 0);
			if (containsAcid(east3)) {
				continue;
			}

			WorldPoint east4 = new WorldPoint(w.getX() + 4, w.getY(), 0);
			if (containsAcid(east4)) {
				continue;
			}

			WorldPoint east5 = new WorldPoint(w.getX() + 5, w.getY(), 0);
			if (containsAcid(east5)) {
				continue;
			}

			return w;

		}
		return null;
	}

	public WorldPoint getSortedPoint() {
		ArrayList<WorldPoint> wl = this.getStartingPointAcid();
		if (wl != null && wl.size() > 0) {
			Collections.sort(wl, new Comparator<WorldPoint>() {
				@Override
				public int compare(WorldPoint z1, WorldPoint z2) {
					if (z1.distanceTo(ctx.players.getLocal().getLocation()) > z2
							.distanceTo(ctx.players.getLocal().getLocation()))
						return 1;
					if (z1.distanceTo(ctx.players.getLocal().getLocation()) < z2
							.distanceTo(ctx.players.getLocal().getLocation()))
						return -1;
					return 0;
				}
			});
			return wl.get(0);
		}
		return null;
	}

	public ArrayList<WorldPoint> getStartingPointAcid() {
		if (!isAcidLanding()) {
			return null;
		}

		ArrayList<WorldPoint> safe = new ArrayList<WorldPoint>();
		WorldArea a = Variables.vorkath.getArea();

		for (WorldPoint w : a.getWorldPoints()) {
			if (w.distanceTo(ctx.players.getLocal().getLocation()) >= 8) {
				continue;
			}

			if (containsAcid(w)) {
				continue;
			}

			WorldPoint east1 = new WorldPoint(w.getX() + 1, w.getY(), 0);
			if (containsAcid(east1)) {
				continue;
			}

			WorldPoint east2 = new WorldPoint(w.getX() + 2, w.getY(), 0);
			if (containsAcid(east2)) {
				continue;
			}

			WorldPoint east3 = new WorldPoint(w.getX() + 3, w.getY(), 0);
			if (containsAcid(east3)) {
				continue;
			}

			WorldPoint east4 = new WorldPoint(w.getX() + 4, w.getY(), 0);
			if (containsAcid(east4)) {
				continue;
			}

			safe.add(w);

		}
		return safe;
	}

	public boolean containsAcid(WorldPoint w) {
		SimpleEntityQuery<SimpleObject> acids = ctx.objects.populate().filter(e -> e.getId() == Constants.ACID_TILE_ID)
				.filter(w);
		if (acids.size() > 0) {
			return true;
		}
		return false;
	}
}