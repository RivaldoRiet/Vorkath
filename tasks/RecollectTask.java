package Vorkath.tasks;

import java.util.Arrays;

import Vorkath.Main;
import Vorkath.data.Constants;
import Vorkath.data.Variables;
import Vorkath.data.utils.Utils;
import simple.hooks.filters.SimpleEquipment.EquipmentSlot;
import simple.hooks.filters.SimpleSkills.Skills;
import simple.hooks.queries.SimpleItemQuery;
import simple.hooks.scripts.task.Task;
import simple.hooks.wrappers.SimpleItem;
import simple.hooks.wrappers.SimpleNpc;
import simple.hooks.wrappers.SimpleObject;
import simple.hooks.wrappers.SimpleWidget;
import simple.robot.api.ClientContext;

public class RecollectTask extends Task {
	private Main main;

	public RecollectTask(ClientContext ctx, Main main) {
		super(ctx);
		this.main = main;
	}

	@Override
	public boolean condition() {
		return Variables.RECOLLECT_ITEMS || Variables.FORCE_BANK;
	}

	@Override
	public void run() {
		Variables.STATUS = "Recollecting items";
		if (Variables.RECOLLECT_ITEMS) {
			Variables.FORCE_BANK = false;
			if (collectedAllItems()) {
				Variables.FORCE_BANK = true;
				Variables.RECOLLECT_ITEMS = false;
				return;
			}

			//ctx.log("amount of items: " + getAmountOfItemsInRecoverScreen() + " - isrecoverscreenopen: " + isRecoverScreenOpen() + " - isPayscreenOpen: " + isPayscreenOpen() );
			if (!Constants.VORKATH_START_AREA.containsPoint(ctx.players.getLocal().getLocation())) {
				if (Variables.teleporter.open()) {
					Variables.STATUS = "Recollecting: Teleport to vorkath";
					if (Variables.teleporter.teleportStringPath("Bossing", "Vorkath")) {
						ctx.sleep(150, 400);
					}
				}
			} else if (inventoryIsFull()) {
				if (this.isRecoverScreenOpen()) {
					this.clickRecoverClose();
				} else {
					dropJunk("Manta ray");
					dropJunk("Shark");
					dropJunk("Anglerfish");
					dropJunk("Pineapple pizza");
				}
			} else if (!this.isRecoverScreenOpen()) {
				ctx.log("Opening recoverscreen");
				openRecoverScreen();
			} else if (this.isRecoverScreenOpen() && getAmountOfItemsInRecoverScreen() > 0 && isPayscreenOpen()) {
				ctx.log("Confirming payment");
				// confirm payment
				confirmPayment();
			} else if (this.isRecoverScreenOpen() && getAmountOfItemsInRecoverScreen() > 0 && !isPayscreenOpen()) {
				this.clickRecoverButton();
			} else if (this.isRecoverScreenOpen() && getAmountOfItemsInRecoverScreen() == 0) {
				if (collectedAllItems()) {
					Variables.FORCE_BANK = true;
					Variables.RECOLLECT_ITEMS = false;
					return;
				}
				openRecoverScreen();

				if (ctx.inventory.populate().population() == 28) {
					dropJunk("Manta ray");
					dropJunk("Shark");
					dropJunk("Anglerfish");
					dropJunk("Pineapple pizza");
				}
			}
		} else if (Variables.FORCE_BANK) {
			if (!isGeared()) {
				if (!Constants.EDGEVILE_AREA.containsPoint(ctx.players.getLocal().getLocation())) {
					ctx.magic.castHomeTeleport();
					ctx.onCondition(() -> Constants.EDGEVILE_AREA.containsPoint(ctx.players.getLocal().getLocation()),
							2500);
				} else {
					SimpleObject bank = ctx.objects.populate().filter(10355).nearest().next();
					Variables.STATUS = "Recollecting: Getting last preset";
					bank.click("Last-preset");
					ctx.sleepCondition(() -> ctx.inventory.inventoryFull(), 1500);
				}
			} else {
				Variables.FORCE_BANK = false;
			}
		}
	}
	
	private void dropJunk(String name) {
		SimpleItemQuery<SimpleItem> items = ctx.inventory.populate().filter(o -> o.getName().toLowerCase().contains(name.toLowerCase()))
				.filterHasAction("Drop");
		for (SimpleItem item : items) {
			ctx.keyboard.pressKey(16);
			item.click(0);
			ctx.keyboard.pressKey(16);
		}
		ctx.sleep(100);
		ctx.keyboard.releaseKey(16);
	}
	

	public void eatFood() {
		SimpleItem food = ctx.inventory.populate().filterHasAction("Eat").next();

		if (food != null) {
			Variables.STATUS = "Clicking food";
			food.click(0);
		}
	}

	private boolean isGeared() {
		boolean equipped = Arrays.asList(EquipmentSlot.SHIELD, EquipmentSlot.WEAPON, EquipmentSlot.AMULET,
				EquipmentSlot.BODY, EquipmentSlot.LEGS).stream().allMatch(pos -> {
					return ctx.equipment.getEquippedItem(pos) != null;
				});

		return equipped;
	}

	private int getAmountOfItemsInRecoverScreen() {
		int amount = 0;
		if (this.isRecoverScreenOpen()) {
			SimpleWidget w = ctx.widgets.getWidget(602, 3); // container screen
			if (w != null) {
				SimpleWidget[] c = w.getChildren(); // child
				for (SimpleWidget wc : c) {
					if (wc != null && wc.visibleOnScreen()) {
						amount++;
					}
				}
			}
		}
		return amount;
	}

	private void openRecoverScreen() {
		if (this.isRecoverScreenOpen()) {
			this.clickRecoverClose();
		}
		SimpleNpc torfin = ctx.npcs.populate().filter("Torfinn").next();
		if (torfin != null) {
			if (torfin.validateInteractable()) {
				torfin.click("Collect");
				ctx.onCondition(() -> isRecoverScreenOpen(), 3500);
			}
		}
	}

	private void clickRecoverButton() {
		SimpleWidget w = ctx.widgets.getWidget(602, 6); // screen
		if (w != null) {
			Variables.STATUS = "Recovering items";
			w.click(0);
		}
	}

	private void clickRecoverClose() {
		SimpleWidget w = ctx.widgets.getWidget(602, 1); // screen
		if (w != null) {
			// text
			SimpleWidget t = ctx.widgets.getWidget(602, 1).getChild(11);
			if (t != null) {
				t.click(0);
				Variables.STATUS = "Closing screen";
				ctx.onCondition(() -> !this.isRecoverScreenOpen(), 3500);
			}
		}
	}

	private boolean isRecoverScreenOpen() {
		SimpleWidget w = ctx.widgets.getWidget(602, 1); // screen
		if (w != null) {
			// text
			SimpleWidget t = ctx.widgets.getWidget(602, 1).getChild(1);
			if (t != null && t.getText().contains("Retrieval Service")) {
				return true;
			}
		}
		return false;
	}

	private boolean collectedAllItems() {
		SimpleWidget w = ctx.widgets.getWidget(229, 1); // screen
		if (w != null) {
			// text
			if (!w.isHidden() && w.getText().contains("are no items for you")) {
				return true;
			}
		}
		return false;
	}

	private boolean isPayscreenOpen() {
		SimpleWidget w = ctx.widgets.getWidget(219, 1); // screen
		if (w != null) {
			// text
			SimpleWidget t = ctx.widgets.getWidget(219, 1).getChild(0);
			if (t != null && t.getText().contains("unlock your item")) {
				return true;
			}
		}
		return false;
	}

	private void confirmPayment() {
		SimpleWidget w = ctx.widgets.getWidget(219, 1); // screen
		if (w != null) {
			// text
			SimpleWidget c = ctx.widgets.getWidget(219, 1).getChild(1);
			if (c != null) {
				c.click(0);
			}
		}
	}

	private boolean inventoryIsFull() {
		return ctx.inventory.populate().population() == 28;
	}

	@Override
	public String status() {
		return "Restocking";
	}

}
