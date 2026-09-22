package com.hbm.inventory.container;

import com.hbm.inventory.SlotNonRetarded;
import com.hbm.inventory.SlotTakeOnly;
import com.hbm.items.ItemVOTVdrive;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRocket;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerLaunchPadRocket extends ContainerBase {

	private static final int SYNC_DRIVE_AUTHORIZATION = 0x4D;

	private final TileEntityLaunchPadRocket launchpad;
	private final EntityPlayer viewingPlayer;

	// -1 = awaiting the server's player-specific answer, 0 = clear, 1 = denied.
	private int driveAuthorizationState = -1;
	private int lastDriveAuthorizationState = Integer.MIN_VALUE;

	public ContainerLaunchPadRocket(InventoryPlayer invPlayer, TileEntityLaunchPadRocket machine) {
		super(invPlayer, machine);
		this.launchpad = machine;
		this.viewingPlayer = invPlayer.player;
		
		addSlotToContainer(new SlotNonRetarded(machine, 0, 37, 21)); // Rocket slot

		// Keep player insertion deterministic on both logical sides. Authorization is
		// player-specific, so it is synchronized by this container rather than being
		// enforced from Slot#isItemValid (which causes client/server slot desync).
		addSlotToContainer(new SlotNonRetarded(machine, 1, 37, 39)); // Drive slot
		
		addSlotToContainer(new SlotNonRetarded(machine, 2, 167, 90)); // Battery slot

		// Preserve the launch pad's legacy fluid-container input behaviour. The tile
		// itself only advertises solid rocket fuel through isItemValidForSlot(3), but
		// FluidTank#loadTank intentionally consumes normal/infinite fluid containers
		// from this same slot. Reject only destination drives here so a failed
		// shift-click can never spill one into the fuel path.
		addSlotToContainer(new Slot(machine, 3, 77, 21) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return stack != null && !ItemVOTVdrive.isUsableDrive(stack);
			}
		}); // Input
		addSlotToContainer(new SlotTakeOnly(machine, 4, 77, 39)); // Output

		addSlots(invPlayer, 9, 14, 154, 3, 9); // Player inventory
		addSlots(invPlayer, 0, 14, 212, 1, 9); // Player hotbar
	}

	private int computeDriveAuthorizationState() {
		if(viewingPlayer == null || viewingPlayer.worldObj == null || viewingPlayer.worldObj.isRemote) {
			return driveAuthorizationState;
		}

		ItemStack drive = launchpad.getStackInSlot(1);
		if(!ItemVOTVdrive.isUsableDrive(drive)) return 0;

		return ItemVOTVdrive.canPlayerUseStationDriveForLaunch(
			drive, launchpad.getWorldObj(), viewingPlayer.getUniqueID()) ? 0 : 1;
	}

	@Override
	public void addCraftingToCrafters(ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
		int state = computeDriveAuthorizationState();
		driveAuthorizationState = state;
		lastDriveAuthorizationState = state;
		crafting.sendProgressBarUpdate(this, SYNC_DRIVE_AUTHORIZATION, state);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

		int state = computeDriveAuthorizationState();
		if(state != lastDriveAuthorizationState) {
			for(int i = 0; i < this.crafters.size(); i++) {
				((ICrafting)this.crafters.get(i)).sendProgressBarUpdate(this, SYNC_DRIVE_AUTHORIZATION, state);
			}
		}

		driveAuthorizationState = state;
		lastDriveAuthorizationState = state;
	}

	@Override
	public void updateProgressBar(int id, int value) {
		if(id == SYNC_DRIVE_AUTHORIZATION) {
			driveAuthorizationState = value;
		}
	}

	public boolean hasDriveAuthorizationIssue() {
		return driveAuthorizationState > 0;
	}

	/**
	 * Conservative client presentation: an unresolved player-specific answer is
	 * never allowed to paint the launch indicator green or send COMMIT.
	 */
	public boolean isDriveAuthorizationClear() {
		return driveAuthorizationState == 0;
	}
}
