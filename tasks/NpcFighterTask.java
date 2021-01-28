package Vorkath.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Vorkath.Main;
import Vorkath.data.Constants;
import Vorkath.data.Variables;
import Vorkath.data.utils.Utils;
import net.runelite.api.VarPlayer;
import net.runelite.api.coords.WorldPoint;
import simple.hooks.filters.SimplePrayers;
import simple.hooks.filters.SimplePrayers.Prayers;
import simple.hooks.filters.SimpleSkills;
import simple.hooks.filters.SimpleEquipment.EquipmentSlot;
import simple.hooks.queries.SimpleEntityQuery;
import simple.hooks.queries.SimpleQuery;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleGroundItem;
import simple.hooks.wrappers.SimpleItem;
import simple.hooks.wrappers.SimpleNpc;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;
import simple.robot.utils.WorldArea;

public class NpcFighterTask extends Task {
	private Main main;
	public WorldArea edge = new WorldArea(new WorldPoint(3074, 3466, 0), new WorldPoint(3126, 3523, 0));
	private long lastFireBallMovement = 0;
	public List<WorldPoint> acidFreePath = new ArrayList<>();
	List<WorldPoint> acidSpots = new ArrayList<>();
	WorldPoint[] walkPath1 = new WorldPoint[5];
	WorldPoint[] walkPath2 = new WorldPoint[5];
	public WorldPoint startTile = null;
	public WorldPoint endTile = null;
	public WorldPoint[] path = null;
	private boolean switchAttackArea = false;
	private int westWalk = 1;

	public NpcFighterTask(ClientContext ctx, Main main) {
		super(ctx);
		this.main = main;
	}

	@Override
	public boolean condition() {
		return !Variables.RECOLLECT_ITEMS && Variables.vorkath.inInstance();
	}

	boolean looted = false;

	@Override
	public void run() {

		SimpleNpc vorkath_sleeping = Variables.vorkath.get(true);

		if (vorkath_sleeping != null) {
			if (shouldLoot()) {
				if (!looted)
					looted = true;
				loot();
			} else if (looted) {
				Variables.STATUS = "Teleporting after loot";
				teleportOut();
			} else {
				looted = false;
				setupVorkath(vorkath_sleeping);
			}

		} else if (Variables.vorkath.isActive()) {
			Variables.STATUS = "Handling vorkath";

			respondToFireball();

			if (Variables.vorkath.isZombieSpawnActive() || this.zombifiedExist()) {
				//Variables.STATUS = "Zombified active";
				
				if (this.hasSlayerStaff() && !this.isWearingSlayerStaff()) {
					Variables.STATUS = "Equipping slayer staff";
					this.equipSlayerStaff();
				}
				if (ctx.players.getLocal().getInteracting() != null && ctx.players.getLocal().getInteracting().getName() != null) {
					//ctx.log("name: " + ctx.players.getLocal().getInteracting().getName());
					if (ctx.players.getLocal().getInteracting().getName().toLowerCase().contains("vorkath")) {
						// make sure the player stops ranging vorkath so the speed doesn't mess up
						ctx.pathing.step(ctx.players.getLocal().getLocation());
					}
				}
				zombified();
				return;
			}else {
				if (this.isWearingSlayerStaff()) {
					Variables.STATUS = "Equipping crossbow";
					 equipCrossbow();
				}
			}

			if (extremelyLowHealth()
					|| (quiteLowHealth() && ctx.inventory.populate().filterHasAction("Eat").population() == 0))
				teleportOut();
			respondToFireball();
			if (isPoisonedOrVenomed())
				Variables.supplies.drinkAntiPoison();
			respondToFireball();
			switchPrayerOnVorkath();
			if (lowHealth())
				Variables.supplies.eatFood();
			respondToFireball();
			if (isAcidLanding()) {
				WorldPoint w = Variables.vorkath.getSortedPoint();
				Variables.STATUS = "Handling Acid";
				handleAcidCustom(w);
			}
			if (lowPrayer())
				drinkPrayerPot();
			respondToFireball();
			if (Variables.supplies.shouldDrinkRangePot())
				Variables.supplies.drinkRangedPotion();
			if (Variables.supplies.shouldDrinkDefencePot())
				Variables.supplies.drinkDefencePotion();
			respondToFireball();
			if (!Variables.vorkath.isFireBallActive() && !isAcidLanding() && inFightArea()
					&& !zombifiedExist())
				attackVorkath();
			if (isAcidLanding()) {
				WorldPoint w = Variables.vorkath.getSortedPoint();
				Variables.STATUS = "Handling Acid";
				handleAcidCustom(w);
			}
			
			respondToFireball();
			
			switchPrayerOnVorkath();
			respondToFireball();
			switchBoltsOnVorkath();
			respondToFireball();
		}

	}
	
	private void equipCrossbow()
	{
		if (Utils.inventoryContains("crossbow")) {
			SimpleItem staff = ctx.inventory.populate().filter(s -> s.getName().toLowerCase().contains("crossbow")).next();
			if (staff != null) {
				staff.click(0);
				ctx.onCondition(() -> isWearingCrossbow(), 500);
			}
		}
	}
	
	private void equipSlayerStaff()
	{
		if (Utils.inventoryContains("slayer")) {
			SimpleItem staff = ctx.inventory.populate().filter(s -> s.getName().toLowerCase().contains("slayer")).next();
			if (staff != null) {
				staff.click(0);
				ctx.onCondition(() -> isWearingSlayerStaff(), 500);
			}
		}
	}
	
	private boolean hasSlayerStaff()
	{
		if(ctx.equipment.getEquippedItem(EquipmentSlot.WEAPON).getName().toLowerCase().contains("slayer"))
		{
			return true;
		}
		if (Utils.inventoryContains("slayer")) {
			return true;
		}
		return false;
	}
	
	private boolean isWearingCrossbow()
	{
		if(ctx.equipment.getEquippedItem(EquipmentSlot.WEAPON).getName().toLowerCase().contains("crossbow"))
		{
			return true;
		}
		return false;
	}

	
	private boolean isWearingSlayerStaff()
	{
		if(ctx.equipment.getEquippedItem(EquipmentSlot.WEAPON).getName().toLowerCase().contains("slayer"))
		{
			return true;
		}
		return false;
	}

	private void respondToFireball()
	{
		//if (!ctx.pathing.inMotion() && Variables.vorkath.isFireBallActive() && (System.currentTimeMillis() - Variables.vorkath.lastFireBallMove) >= 1000) {
		if (Variables.vorkath.isFireBallActive() && (System.currentTimeMillis() - Variables.vorkath.lastFireBallMove) >= 2000) {
			Variables.STATUS = "Fire ball active";
			handleFireball();
		}
	}
	
	private void setupVorkath(SimpleNpc vorkath_sleeping) {
		if (inventWithBones()) {
			Variables.STATUS = "Teleporting away";
			ctx.magic.castHomeTeleport();
		}else {
			Variables.STATUS = "Waking up vorkath";
			if (vorkath_sleeping.click("Poke")) {
				Utils.setZoom(1);
				WorldPoint initial = ctx.players.getLocal().getLocation();
				ctx.sleepCondition(() -> vorkath_sleeping.distanceTo(ctx.players.getLocal()) <= 5, 5000);
				ctx.sleep(1000);
				switchPrayerOnVorkath();
				Variables.supplies.drinkAntiFire();
				WorldPoint newWP = new WorldPoint(initial.getX(), initial.getY() - 5, initial.getPlane());
				WorldPoint[] path = { newWP };
				ctx.pathing.walkPath(path);
			}
		}
	}


	private boolean inventWithBones() {
		if (Utils.inventoryContains("Dragon bones")) {
			return true;
		}
		return false;
	}
	
	private boolean zombifiedExist() {
		SimpleNpc zombified = ctx.npcs.populate().filter("Zombified spawn").filter(n -> !n.isDead()).next();
		if (zombified != null) {
			return true;
		}
		return false;
	}

	private boolean zombified() {		
		SimpleNpc zombified = ctx.npcs.populate().filter("Zombified spawn").nearest().next();
		if (zombified == null)
			return false;

		//if (!zombified.isDead()) {
			Variables.STATUS = "Zombified active";
			if (this.isWearingSlayerStaff()) {
				Variables.STATUS = "Crumble undeath w/ staff";
				zombified.click("Attack");
				return true;
			}else {
			if (ctx.magic.castSpellOnNPC("Crumble Undead", zombified)) {
				return true;
			}
			}
	//	}
		return false;
	}

	private boolean lowPrayer() {
		return ctx.skills.level(SimpleSkills.Skills.PRAYER) <= 30;
	}

	public void drinkPrayerPot() {
		SimpleItem prayerPot = ctx.inventory.populate().filter(Constants.PRAYER_POT_IDS).next();
		if (prayerPot != null) {
			Variables.STATUS = "Clicking prayer potion";
			prayerPot.click(0);
		}
	}

	public boolean shouldLoot() {
		return ctx.groundItems.populate().filter(n -> !Constants.VORKATH_START_AREA.containsPoint(n.getLocation())).size() > 0;
	}

	private boolean inFightArea() {
		return true;
		/*
		WorldArea w = Variables.vorkath.rangeAttackArea();
		if (w != null && w.containsPoint(ctx.players.getLocal().getLocation())) {
			return true;
		}
		return false;*/
	}

	private void walkBackToFightArea() {
		WorldArea w = Variables.vorkath.rangeAttackArea();
		if (w != null && w.getWorldPoints().length > 0 && !Variables.vorkath.isFireBallActive()) {
			Variables.STATUS = "Walking back to attack area";
			ctx.pathing.step(w.randomTile());
		}
	}

	private void switchPrayerOnVorkath() {
		respondToFireball();
		Utils.enablePrayer(Prayers.PROTECT_FROM_MAGIC);
		respondToFireball();
		Utils.enablePrayer(SimplePrayers.Prayers.EAGLE_EYE);
		respondToFireball();
	}

	private void switchBoltsOnVorkath() {
		int vorkathHealth = Variables.vorkath.getVorkathHealth();
		if (vorkathHealth == -1) {
			return;
		}

		// 30 is the max health ratio
		if (Utils.inventoryContains("Ruby bolts (e)") && vorkathHealth >= 35) {
			SimpleItem bolts = Utils.getItem("Ruby bolts (e)");
			if (bolts != null) {
				Variables.STATUS = "Switching bolts";
				bolts.click(0);
				ctx.sleep(501);
			}
		} else if (Utils.inventoryContains("Diamond bolts (e)") && vorkathHealth < 35) {
			SimpleItem bolts = Utils.getItem("Diamond bolts (e)");
			if (bolts != null) {
				Variables.STATUS = "Switching diamond bolts";
				bolts.click(0);
				ctx.sleep(500, 600);
			}
		}
	}

	private void handleFireball() {

		if (Variables.vorkath.isFireBallActive()) {
			Variables.STATUS = "Fire ball active";
			WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();
			WorldPoint westSpot = new WorldPoint(startingSpot.getX() - 2, startingSpot.getY(),
					startingSpot.getPlane());
			WorldPoint eastSpot = new WorldPoint(startingSpot.getX() + 2, startingSpot.getY(),
					startingSpot.getPlane());

			
			if (ctx.pathing.reachable(westSpot) && ctx.pathing.reachable(eastSpot)) {
				if (westWalk == 1 || westWalk == 2) {
					walkFireBall(westSpot);
				} else if (westWalk == 3 || westWalk == 4) {
					walkFireBall(eastSpot);
				}
				return;
			}
			
			if (ctx.pathing.reachable(westSpot)) {
				walkFireBall(westSpot);
			} else {
				walkFireBall(eastSpot);
			}
		}
	}
	
	private void walkFireBall(WorldPoint w)
	{
		if (ctx.pathing.reachable(w)) {
			ctx.pathing.step(w);
			ctx.onCondition(() -> ctx.players.getLocal().getLocation().equals(w), 500);
			if (!ctx.players.getLocal().getLocation().equals(w)) {
				ctx.pathing.step(w);
				ctx.onCondition(() -> ctx.players.getLocal().getLocation().equals(w), 500);
				if (!ctx.players.getLocal().getLocation().equals(w)) {
					ctx.pathing.step(w);
					ctx.onCondition(() -> ctx.players.getLocal().getLocation().equals(w), 500);
				}
			}
			Variables.vorkath.lastFireBallMove = System.currentTimeMillis();
			westWalk++;
			if (westWalk >= 5) {
				westWalk = 1;
			}
			attackVorkath();
			//ctx.onCondition(() -> !Variables.vorkath.isFireBallActive(), 3500);
		}
	}
	/*
	 * private void handleFireball() { if (System.currentTimeMillis() -
	 * this.lastFireBallMovement >= 5000) { if
	 * (Variables.vorkath.isFireBallActive()) { WorldPoint startingSpot =
	 * ctx.players.getLocal().getPlayer().getWorldLocation(); WorldPoint westSpot =
	 * new WorldPoint(startingSpot.getX() - 2, startingSpot.getY(),
	 * startingSpot.getPlane()); WorldPoint eastSpot = new
	 * WorldPoint(startingSpot.getX() + 2, startingSpot.getY(),
	 * startingSpot.getPlane()); Random r = new Random(); int choice = r.nextInt(2);
	 * if (choice == 0) { if (ctx.pathing.reachable(westSpot)) {
	 * ctx.pathing.step(westSpot); } else { ctx.pathing.step(eastSpot); }
	 * ctx.sleepCondition(() -> !Variables.vorkath.isFireBallActive(), 5000); } else
	 * { if (ctx.pathing.reachable(eastSpot)) { ctx.pathing.step(eastSpot); } else {
	 * ctx.pathing.step(westSpot); } ctx.sleepCondition(() ->
	 * !Variables.vorkath.isFireBallActive(), 5000); } } } }
	 */

	public void attackVorkath() {
		SimpleNpc vorkath = Variables.vorkath.get();
		if (ctx.getClient().getSpellSelected()) {
			Variables.STATUS = "Deselecting spell";
			ctx.pathing.step(ctx.players.getLocal().getLocation());
		}

		if (vorkath != null && ctx.players.getLocal().getInteracting() == null) {
			Variables.STATUS = "Attacking vorkath";
			vorkath.click("Attack");
		}
	}

	public boolean isPoisonedOrVenomed() {
		return ctx.getClient().getVar(VarPlayer.IS_POISONED) >= 30;
	}

	public void loot() {
		Variables.STATUS = "Looting";
		if (ctx.inventory.populate().population() == 28) {
			Variables.supplies.eatFood();
		}

		SimpleGroundItem loot = ctx.groundItems.populate().next();
		if (loot != null && ctx.inventory.populate().population() < 28) {
			if (loot.validateInteractable()) {
				loot.click("Take");
			}
		}
	}

	public boolean lowHealth() {
		return ctx.players.getLocal().getHealth() <= 77;
	}

	public boolean extremelyLowHealth() {
		return ctx.players.getLocal().getHealth() < 20;
	}

	public boolean quiteLowHealth() {
		return ctx.players.getLocal().getHealth() < 40;
	}

	public void teleportOut() {
		Variables.STATUS = "Low health teleporting out";
		ctx.magic.castHomeTeleport();
		looted = false;
	}

	private boolean isAcidLanding() {
		return ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() > 0 || acidLanding();
	}

	public boolean isAcidPhase2() {
		if (Variables.vorkath.get() == null)
			return false;
		return ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() > 0
				|| Variables.vorkath.get().getAnimation() == Constants.VORKATH_ACID_ATTACK_ANIM_ID
				|| ctx.projectiles.projectileActive(Constants.VORKATH_ACID_ATTACK_PROJECTILE_ID);
	}

	private boolean noAcidExists() {
		return ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() == 0;
	}

	public void handleAcid() {
		SimpleEntityQuery acidObjects = ctx.objects.populate().filter(e -> e.getId() == Constants.ACID_TILE_ID);
		/*
		 * for(SimpleObject a : acidObjects){ } for(SimpleObject p : Sim){ }
		 * ctx.pathing.walkPath();
		 */
	}

	public boolean acidLanding() {
		return ctx.projectiles.projectileActive(1483);
	}

	public void handleAcidBrute() {
		WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();

		WorldPoint westSpot = new WorldPoint(startingSpot.getX() - 5, startingSpot.getY(), startingSpot.getPlane());
		WorldPoint eastSpot = new WorldPoint(startingSpot.getX() + 5, startingSpot.getY(), startingSpot.getPlane());
		WorldPoint[] west = new WorldPoint[] { westSpot };
		WorldPoint[] start = new WorldPoint[] { startingSpot };
		WorldPoint[] east = new WorldPoint[] { eastSpot };
		// ctx.pathing.running(false);
		while (!Variables.RECOLLECT_ITEMS && Variables.vorkath.inInstance() && acidLanding()) {
			if (quiteLowHealth())
				teleportOut();
			if (ctx.pathing.reachable(westSpot)) {
				if (ctx.pathing.distanceTo(startingSpot) <= 1) {
					ctx.pathing.walkPath(west);
				}
				if (ctx.pathing.distanceTo(westSpot) <= 1) {
					ctx.pathing.walkPath(start);
				}
			} else {
				if (ctx.pathing.distanceTo(startingSpot) <= 1) {
					ctx.pathing.walkPath(east);
				}
				if (ctx.pathing.distanceTo(eastSpot) <= 1) {
					ctx.pathing.walkPath(start);
				}
			}

			if (lowHealth())
				Variables.supplies.eatFood();

		}
		ctx.pathing.running(true);
	}

	private void populateAcidSpots() {
		WorldPoint vorkLoc = Variables.vorkath.get().getLocation();
		final int maxX = vorkLoc.getX() + 14;
		final int minX = vorkLoc.getX() - 8;
		final int maxY = vorkLoc.getY() - 1;
		final int minY = vorkLoc.getY() - 8;
		WorldArea pp = new WorldArea(new WorldPoint(minX, minY, vorkLoc.getPlane()),
				new WorldPoint(maxX, maxY, vorkLoc.getPlane()));
		SimpleQuery acids = ctx.objects.populate().filter(e -> e.getId() == Constants.ACID_TILE_ID).filter(pp);
		System.out.println(acids.size());
		for (Object c : acids) {
			try {
				SimpleObject b = (SimpleObject) c;
				acidSpots.add(b.getLocation());
			} catch (Exception e) {

			}
		}
	}

	private void calculateAcidFreePath() {
		acidFreePath.clear();
		if (Variables.vorkath.get() == null) {
			return;
		}

		final int[][][] directions = { { { 0, 1 }, { 0, -1 } // Positive and negative Y
				}, { { 1, 0 }, { -1, 0 } // Positive and negative X
				} };

		List<WorldPoint> bestPath = new ArrayList<>();
		double bestClicksRequired = 99;

		final WorldPoint playerLoc = ctx.players.getLocal().getLocation();
		final WorldPoint vorkLoc = Variables.vorkath.get().getLocation();
		final int maxX = vorkLoc.getX() + 14;
		final int minX = vorkLoc.getX() - 8;
		final int maxY = vorkLoc.getY() - 1;
		final int minY = vorkLoc.getY() - 8;

		// Attempt to search an acid free path, beginning at a location
		// adjacent to the player's location (including diagonals)
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++) {
				final WorldPoint baseLocation = new WorldPoint(playerLoc.getX() + x, playerLoc.getY() + y,
						playerLoc.getPlane());

				if (acidSpots.contains(baseLocation) || baseLocation.getY() < minY || baseLocation.getY() > maxY) {
					continue;
				}

				// Search in X and Y direction
				for (int d = 0; d < directions.length; d++) {
					// Calculate the clicks required to start walking on the path
					double currentClicksRequired = Math.abs(x) + Math.abs(y);
					if (currentClicksRequired < 2) {
						currentClicksRequired += Math.abs(y * directions[d][0][0]) + Math.abs(x * directions[d][0][1]);
					}
					if (d == 0) {
						// Prioritize a path in the X direction (sideways)
						currentClicksRequired += 0.5;
					}

					List<WorldPoint> currentPath = new ArrayList<>();
					currentPath.add(baseLocation);

					// Positive X (first iteration) or positive Y (second iteration)
					for (int i = 1; i < 25; i++) {
						final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][0][0],
								baseLocation.getY() + i * directions[d][0][1], baseLocation.getPlane());

						if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY
								|| testingLocation.getY() > maxY || testingLocation.getX() < minX
								|| testingLocation.getX() > maxX) {
							break;
						}

						currentPath.add(testingLocation);
					}

					// Negative X (first iteration) or positive Y (second iteration)
					for (int i = 1; i < 25; i++) {
						final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][1][0],
								baseLocation.getY() + i * directions[d][1][1], baseLocation.getPlane());

						if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY
								|| testingLocation.getY() > maxY || testingLocation.getX() < minX
								|| testingLocation.getX() > maxX) {
							break;
						}

						currentPath.add(testingLocation);
					}

					if (currentPath.size() >= 5 && currentClicksRequired < bestClicksRequired
							|| (currentClicksRequired == bestClicksRequired && currentPath.size() > bestPath.size())) {
						bestPath = currentPath;
						bestClicksRequired = currentClicksRequired;
					}
				}
			}
		}

		if (bestClicksRequired != 99) {
			acidFreePath = bestPath;
		}
	}

	public boolean isHorizontal(List<WorldPoint> path) {
		if (path.size() < 2)
			return false;
		int currX = path.get(0).getX();
		for (int i = 1; i < path.size(); i++) {
			if (path.get(i).getX() != currX)
				return false;
			currX = path.get(i).getX();
		}
		return true;
	}

	public boolean isVertical(List<WorldPoint> path) {
		if (path.size() < 2)
			return false;
		int currY = path.get(0).getY();
		for (int i = 1; i < path.size(); i++) {
			if (path.get(i).getY() != currY)
				return false;
			currY = path.get(i).getY();
		}
		return true;
	}

	public WorldPoint[] sortVertical(List<WorldPoint> path) {

		int minY = 10000000;
		int maxY = 0;
		for (WorldPoint p : path) {
			if (p.getY() < minY)
				minY = p.getY();
			if (p.getY() > maxY)
				maxY = p.getY();
		}
		int size = path.size();
		int xCoord = path.get(0).getX();
		WorldPoint[] result = new WorldPoint[size];
		for (int i = 0; i < size; i++) {
			result[i] = new WorldPoint(xCoord, minY + i, 0);
		}
		return result;

	}

	public void handleAcidCustom(WorldPoint startingSpot) {
		if (acidLanding())
			handleAcidBrute();

		WorldPoint westSpot = new WorldPoint(startingSpot.getX() - 5, startingSpot.getY(), startingSpot.getPlane());
		WorldPoint eastSpot = new WorldPoint(startingSpot.getX() + 5, startingSpot.getY(), startingSpot.getPlane());
		WorldPoint[] west = new WorldPoint[] { westSpot };
		WorldPoint[] start = new WorldPoint[] { startingSpot };
		WorldPoint[] east = new WorldPoint[] { eastSpot };
		// ctx.pathing.running(false);
		boolean isTileSet = false;
		boolean walkedToSetTile = false;
		while (!Variables.RECOLLECT_ITEMS && Variables.vorkath.inInstance() && isAcidLanding()) {
			Variables.STATUS = "Handling acid pathing";
			if (quiteLowHealth())
				teleportOut();

			if (!isTileSet && ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() > 0) {
				WorldPoint w = Variables.vorkath.getSortedPoint();
				if (w != null) {
					startingSpot = w;
					westSpot = new WorldPoint(startingSpot.getX() - 5, startingSpot.getY(), startingSpot.getPlane());
					eastSpot = new WorldPoint(startingSpot.getX() + 5, startingSpot.getY(), startingSpot.getPlane());
					west = new WorldPoint[] { westSpot };
					start = new WorldPoint[] { startingSpot };
					east = new WorldPoint[] { eastSpot };
					isTileSet = true;
					Variables.vorkath.debugTile = w;
				}
			}

			if (!walkedToSetTile && isTileSet) {
				if (ctx.players.getLocal().getLocation().equals(startingSpot)) {
					walkedToSetTile = true;

				} else {
					ctx.pathing.step(startingSpot);
				}
			}

			if (walkedToSetTile || !isTileSet) {

				if (ctx.pathing.reachable(eastSpot)) {
					if (ctx.pathing.distanceTo(startingSpot) <= 1) {
						ctx.pathing.walkPath(east);
					}
					if (ctx.pathing.distanceTo(eastSpot) <= 1) {
						ctx.pathing.walkPath(start);
					}
				} else {
					if (ctx.pathing.distanceTo(startingSpot) <= 1) {
						ctx.pathing.walkPath(west);
					}
					if (ctx.pathing.distanceTo(westSpot) <= 1) {
						ctx.pathing.walkPath(start);
					}
				}
			}

			// if (lowHealth())
			// Variables.supplies.eatFood();

		}
		Variables.vorkath.debugTile = null;
		ctx.pathing.running(true);
	}

	public WorldPoint[] sortHorizontal(List<WorldPoint> path) {

		int minX = 10000000;
		int maxX = 0;
		for (WorldPoint p : path) {
			if (p.getX() < minX)
				minX = p.getX();
			if (p.getX() > maxX)
				maxX = p.getX();
		}
		int size = path.size();
		int yCoord = path.get(0).getY();
		WorldPoint[] result = new WorldPoint[size];
		for (int i = 0; i < size; i++) {
			result[i] = new WorldPoint(minX + i, yCoord, 0);
		}
		return result;

	}

	public WorldPoint[] buildBrutePath() {
		WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();

		WorldPoint[] result = new WorldPoint[6];
		for (int i = 0; i < result.length; i++) {
			result[i] = new WorldPoint(startingSpot.getX() - i, startingSpot.getY(), startingSpot.getPlane());
		}
		return result;
	}

	@Override
	public String status() {
		return "Fighting vorkath";
	}

}