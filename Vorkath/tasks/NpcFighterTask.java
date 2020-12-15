package Vorkath.tasks;

import Vorkath.Main;
import Vorkath.data.Constants;

import Vorkath.data.Variables;
import Vorkath.data.Vorkath;
import Vorkath.data.utils.Utils;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import simple.hooks.filters.SimplePrayers;
import simple.hooks.filters.SimplePrayers.Prayers;
import simple.hooks.filters.SimpleSkills;
import simple.hooks.queries.SimpleEntityQuery;

import simple.hooks.filters.SimplePrayers.Prayers;

import simple.hooks.queries.SimpleQuery;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleItem;
import simple.hooks.wrappers.SimpleNpc;
import simple.hooks.wrappers.SimpleObject;
import simple.robot.api.ClientContext;
import simple.robot.utils.Random;
import simple.robot.utils.WorldArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NpcFighterTask extends Task {
    private Main main;
    public WorldArea edge = new WorldArea(new WorldPoint(3074, 3466, 0), new WorldPoint(3126, 3523, 0));
    private long lastFireBallMovement = 0;
    public List<WorldPoint> acidFreePath = new ArrayList<>();
    List<WorldPoint> acidSpots = new ArrayList<>();
    WorldPoint[] walkPath1 = new WorldPoint[5];
    WorldPoint[] walkPath2 = new WorldPoint[5];

    public NpcFighterTask(ClientContext ctx, Main main) {
        super(ctx);
        this.main = main;
    }

    @Override
    public boolean condition() {
        return Variables.vorkath.inInstance();
    }

    @Override
    public void run() {
        Variables.STATUS = "In fighter task";
        SimpleNpc vorkath_sleeping = Variables.vorkath.get(true);

        if (vorkath_sleeping != null && vorkath_sleeping.turnTo()) {
            if(shouldLoot()) loot();
            else {
                setupVorkath(vorkath_sleeping);
            }

        } else if (Variables.vorkath.isActive()) {
            switchPrayerOnVorkath();
            switchBoltsOnVorkath();
            if (zombified()) return;

            if (Variables.vorkath.isFireBallActive()) {
                Variables.STATUS = "Fire ball active";
                handleFireball();
            }
            if (extremelyLowHealth()) teleportOut();

            if (isPoisonedOrVenomed()) Variables.supplies.drinkAntiPoision();
            switchPrayerOnVorkath();
            if (lowHealth()) Variables.supplies.eatFood();
            if (isAcidPresent()) {
                Variables.STATUS = "Handling Acid";
                handleAcidSmart();
            }
            if (lowPrayer()) drinkPrayerPot();
            if (shouldDrinkRangePot()) drinkRangedPotion();
            if (!isAcidPresent() && !Variables.vorkath.isFireBallActive() && !zombified() && !inFightArea()) {
                walkBackToFightArea();
            }
            if (Variables.vorkath.isActive() && !Variables.vorkath.isFireBallActive() && !isAcidPresent()) attackVorkath();
        }

    }

    private void setupVorkath(SimpleNpc vorkath_sleeping) {
        vorkath_sleeping.click("Poke");
        WorldPoint initial = ctx.players.getLocal().getLocation();
        ctx.sleepCondition(() -> vorkath_sleeping.distanceTo(ctx.players.getLocal()) <= 5, 5000);
        ctx.sleep(1000);
        switchPrayerOnVorkath();
        Variables.supplies.drinkAntiFire();
        WorldPoint newWP = new WorldPoint(initial.getX(), initial.getY() - 5, initial.getPlane());
        WorldPoint[] path = {newWP};
        ctx.pathing.walkPath(path);
    }

    private boolean zombified() {
        SimpleNpc zombified = ctx.npcs.populate().filter(8062, 8063).nearest().next();
        if (zombified == null) return false;
        if (!zombified.isDead()) {
            if (ctx.players.getLocal().getInteracting() != null) {
                // make sure the player stops ranging vorkath so the speed doesn't mess up
                ctx.pathing.step(ctx.players.getLocal().getLocation());
            }

            Variables.STATUS = "Zombified active";
            if (ctx.magic.castSpellOnNPC("Crumble Undead", zombified))
                ctx.sleep(50, 150);
            return true;
        }
        return false;
    }

    private boolean lowPrayer() {
        return ctx.skills.level(SimpleSkills.Skills.PRAYER) <= 30;
    }

    public void drinkPrayerPot() {
        SimpleItem prayerPot = ctx.inventory.populate().filter(Constants.PRAYER_POT_IDS).next();
        if (prayerPot != null) {
            prayerPot.click(0);
        }
    }

    public boolean shouldLoot(){
        return ctx.groundItems.populate().size() > 0;
    }
    private void handleZombie() {
        SimpleNpc zombified = ctx.npcs.populate().filter(8062, 8063).nearest().next();
        if (zombified != null && !zombified.isDead()) {
            Variables.STATUS = "Zombified active";
            while (zombified != null) {
                try {
                    zombified = ctx.npcs.populate().filter(8062, 8063).nearest().next();
                    if (ctx.magic.castSpellOnNPC("Crumble Undead", zombified)) {
                        SimpleNpc finalZombified = zombified;
                        ctx.sleepCondition(() -> finalZombified.isDead(), 1000);
                    }
                    if (zombified.isDead() || zombified == null) break;
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    private boolean inFightArea() {
        WorldArea w = Variables.vorkath.rangeAttackArea();
        if (w != null && w.containsPoint(ctx.players.getLocal().getLocation())) {
            return true;
        }
        return false;
    }

    private void walkBackToFightArea() {
        WorldArea w = Variables.vorkath.rangeAttackArea();
        Variables.STATUS = "Walking back to attack area";
        if (w != null && w.getWorldPoints().length > 0) {
            ///if (!ctx.pathing.running())
               // ctx.pathing.running(true);
            ctx.pathing.step(w.randomTile());
        }
    }

    private void switchPrayerOnVorkath() {
        Utils.enablePrayer(Prayers.PROTECT_FROM_MAGIC);
        Utils.enablePrayer(SimplePrayers.Prayers.EAGLE_EYE);
        Utils.enablePrayer(Prayers.STEEL_SKIN);
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
                bolts.click(0);
                ctx.sleep(501);
            }
        } else if (Utils.inventoryContains("Diamond bolts (e)") && vorkathHealth < 35) {
            SimpleItem bolts = Utils.getItem("Diamond bolts (e)");
            if (bolts != null) {
                bolts.click(0);
                ctx.sleep(500,600);
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

            if (ctx.pathing.reachable(westSpot)) {
                ctx.pathing.step(westSpot);
            } else {
                ctx.pathing.step(eastSpot);
            }
            ctx.sleep(888,1222);

        }
    }
/*    private void handleFireball() {
        if (System.currentTimeMillis() - this.lastFireBallMovement >= 5000) {
            if (Variables.vorkath.isFireBallActive()) {
                WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();
                WorldPoint westSpot = new WorldPoint(startingSpot.getX() - 2, startingSpot.getY(),
                        startingSpot.getPlane());
                WorldPoint eastSpot = new WorldPoint(startingSpot.getX() + 2, startingSpot.getY(),
                        startingSpot.getPlane());
                Random r = new Random();
                int choice = r.nextInt(2);
                if (choice == 0) {
                    if (ctx.pathing.reachable(westSpot)) {
                        ctx.pathing.step(westSpot);
                    } else {
                        ctx.pathing.step(eastSpot);
                    }
                    ctx.sleepCondition(() -> !Variables.vorkath.isFireBallActive(), 5000);
                } else {
                    if (ctx.pathing.reachable(eastSpot)) {
                        ctx.pathing.step(eastSpot);
                    } else {
                        ctx.pathing.step(westSpot);
                    }
                    ctx.sleepCondition(() -> !Variables.vorkath.isFireBallActive(), 5000);
                }
            }
        }
    }*/

    public void attackVorkath() {
        SimpleNpc vorkath = Variables.vorkath.get();
        if (vorkath != null)
            vorkath.click(0);
    }

    public boolean isPoisonedOrVenomed() {
       return ctx.getClient().getVar(VarPlayer.IS_POISONED) >= 30;
    }

    public void drinkRangedPotion() {
        SimpleItem rangePot = ctx.inventory.populate().filter(Constants.RANGE_POT__IDS).next();
        if (rangePot != null) rangePot.click(0);
    }

    public boolean shouldDrinkRangePot() {
        return ctx.skills.level(SimpleSkills.Skills.RANGED) <= 105;
    }

    public void loot() {
        ctx.groundItems.forEach(e -> {
            if (e != null) e.click(0);
        });
    }

    public boolean lowHealth() {
        return ctx.players.getLocal().getHealth() < 88;
    }

    public boolean extremelyLowHealth() {
        return ctx.players.getLocal().getHealth() < 20;
    }

    public boolean quiteLowHealth() {
        return ctx.players.getLocal().getHealth() < 40;
    }

    public void teleportOut() {
        ctx.magic.castHomeTeleport();
    }


    private boolean isAcidPresent() {
        return ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() > 0 || acidLanding();
    }

    private boolean isAcidPresent2() {
        return ctx.objects.populate().filter(Constants.ACID_TILE_ID).size() > 0 ||
                Variables.vorkath.get().getAnimation() == Constants.VORKATH_ACID_ATTACK_ANIM_ID ||
                ctx.projectiles.projectileActive(Constants.VORKATH_ACID_ATTACK_PROJECTILE_ID);
    }

    public void handleAcid() {
        SimpleEntityQuery acidObjects = ctx.objects.populate().filter(e -> e.getId() == Constants.ACID_TILE_ID);
		/*for(SimpleObject a : acidObjects){
		}
		for(SimpleObject p : Sim){
		}
		ctx.pathing.walkPath();*/
    }

    public boolean acidLanding() {
        return ctx.projectiles.projectileActive(1483);
    }

    public void handleAcidBrute() {
        WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();

        WorldPoint westSpot = new WorldPoint(startingSpot.getX() - 5, startingSpot.getY(), startingSpot.getPlane());
        WorldPoint eastSpot = new WorldPoint(startingSpot.getX() + 5, startingSpot.getY(), startingSpot.getPlane());
        WorldPoint[] west = new WorldPoint[]{westSpot};
        WorldPoint[] start = new WorldPoint[]{startingSpot};
        WorldPoint[] east = new WorldPoint[]{eastSpot};
        ctx.pathing.running(false);
        while (isAcidPresent()) {
            if (quiteLowHealth()) teleportOut();
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

            if (lowHealth()) Variables.supplies.eatFood();

        }
        ctx.pathing.running(true);
    }

    private void setPrayer(SimplePrayers.Prayers p) {
        if (!isPrayerOn(p)) {
            ctx.prayers.prayer(p);
        }
    }

    private boolean isPrayerOn(SimplePrayers.Prayers p) {
        return ctx.prayers.prayerActive(p);
    }


    private void populateAcidSpots() {
        WorldPoint vorkLoc = Variables.vorkath.get().getLocation();
        final int maxX = vorkLoc.getX() + 14;
        final int minX = vorkLoc.getX() - 8;
        final int maxY = vorkLoc.getY() - 1;
        final int minY = vorkLoc.getY() - 8;
        WorldArea pp = new WorldArea(new WorldPoint(minX, minY, vorkLoc.getPlane()), new WorldPoint(maxX, maxY, vorkLoc.getPlane()));
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

        final int[][][] directions = {
                {
                        {0, 1}, {0, -1} // Positive and negative Y
                },
                {
                        {1, 0}, {-1, 0} // Positive and negative X
                }
        };

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
                final WorldPoint baseLocation = new WorldPoint(playerLoc.getX() + x,
                        playerLoc.getY() + y, playerLoc.getPlane());

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

                        if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
                                || testingLocation.getX() < minX || testingLocation.getX() > maxX) {
                            break;
                        }

                        currentPath.add(testingLocation);
                    }

                    // Negative X (first iteration) or positive Y (second iteration)
                    for (int i = 1; i < 25; i++) {
                        final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][1][0],
                                baseLocation.getY() + i * directions[d][1][1], baseLocation.getPlane());

                        if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
                                || testingLocation.getX() < minX || testingLocation.getX() > maxX) {
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

    public void handleAcidSmart() {
        ctx.pathing.running(false);
/*        WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();
        WorldPoint westSpot = new WorldPoint(startingSpot.getX() - 5, startingSpot.getY(), startingSpot.getPlane());
        WorldPoint eastSpot = new WorldPoint(startingSpot.getX() + 5, startingSpot.getY(), startingSpot.getPlane());
        WorldPoint[] west = new WorldPoint[]{westSpot};
        while (acidLanding()) {
            System.out.println("IN FIRST LOOP");
            ctx.sleep(10);
            if (extremelyLowHealth()) teleportOut();
            if (ctx.pathing.reachable(westSpot)) ctx.pathing.walkPath(west);
        }*/
        WorldPoint startingSpot = ctx.players.getLocal().getPlayer().getWorldLocation();
        WorldPoint middleSpot = Variables.vorkath.rangeAttackArea().randomTile();
        ctx.pathing.step(middleSpot);


        populateAcidSpots();
        calculateAcidFreePath();

        System.out.println("Size of acid free path is: " + acidFreePath.size());

        walkPath1 = new WorldPoint[acidFreePath.size()];
        int i = 0;
        for (WorldPoint p : acidFreePath) {
            walkPath1[i++] = p;
        }


        List<WorldPoint> reversed = new ArrayList<>();
        for (WorldPoint p : acidFreePath) {
            reversed.add(p);
        }
        Collections.reverse(reversed);

        walkPath2 = new WorldPoint[acidFreePath.size()];
        i = 0;
        for (WorldPoint p : reversed) {
            walkPath2[i++] = p;
        }

        WorldPoint startSpot1 = walkPath1[0];
        WorldPoint startSpot2 = walkPath2[0];

        System.out.println("Calculated path length: " + walkPath1.length);
        if(walkPath1.length >= 4) {
            while (isAcidPresent2()) {
                // System.out.println("IN SECOND LOOP");
                if (quiteLowHealth()) teleportOut();
                if (ctx.pathing.distanceTo(startSpot1) <= 3) {

                    ctx.pathing.walkPath(walkPath1);
                    System.out.println("WALKING FORWARD PATH");
                    ctx.sleep(100,150);
                }
                else if (ctx.pathing.distanceTo(startSpot2) <= 3) {

                    ctx.pathing.walkPath(walkPath2);
                    System.out.println("WALKING REVERSE PATH");
                    ctx.sleep(100,150);
                }


              if (lowHealth()) Variables.supplies.eatFood();
            }
        }else{
            handleAcidBrute();
        }
        if (!isAcidPresent()) {
            acidSpots.clear();
            acidFreePath.clear();
            ctx.pathing.running(true);
        }


    }

    @Override
    public String status() {
        return "Fighting vorkath";
    }

}