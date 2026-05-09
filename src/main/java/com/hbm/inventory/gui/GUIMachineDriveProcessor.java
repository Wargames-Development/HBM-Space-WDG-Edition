package com.hbm.inventory.gui;

import com.hbm.items.ModItems;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerDriveProcessor;
import com.hbm.lib.RefStrings;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.machine.TileEntityMachineDriveProcessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GUIMachineDriveProcessor extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation( RefStrings.MODID + ":textures/gui/processing/gui_drive_processor.png");

	private TileEntityMachineDriveProcessor machine;

	public GUIMachineDriveProcessor(InventoryPlayer invPlayer, TileEntityMachineDriveProcessor machine) {
		super(new ContainerDriveProcessor(invPlayer, machine));
		this.machine = machine;

		this.xSize = 176;
		this.ySize = 207;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 148, guiTop + 18, 16, 52, machine.power, machine.maxPower);

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 63, guiTop + 17, 18, 18, mouseX, mouseY, new String[] {"Clone drive"} );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 52, guiTop + 61, 18, 18, mouseX, mouseY, new String[] {"Start drive processing"} );

		if(this.mc.thePlayer.inventory.getItemStack() == null && this.isMouseOverSlot(this.inventorySlots.getSlot(4), mouseX, mouseY) && !this.inventorySlots.getSlot(4).getHasStack()) {
			ItemStack[] list = new ItemStack[] { new ItemStack(ModItems.designator), new ItemStack(ModItems.designator_range), new ItemStack(ModItems.designator_manual) };
			List<Object[]> lines = new ArrayList();
			ItemStack selected = list[(int) ((System.currentTimeMillis() % (1000 * list.length)) / 1000)];
			selected.stackSize = 0;
			lines.add(list);

			lines.add(new Object[] {I18nUtil.resolveKey(selected.getDisplayName())});
			this.drawStackText(lines, mouseX, mouseY, this.fontRendererObj);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float interp, int mX, int mY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(machine.isProcessing) {
			drawTexturedModalRect(guiLeft + 42, guiTop + 61, 192, 28, 18, 18);
		}

		int p = (int) (machine.power * 52 / machine.maxPower);
		drawTexturedModalRect(guiLeft + 148, guiTop + 18 + 52 - p, xSize, 52 - p, 16, p);

		if(machine.isProcessing) {
			p = (int) (machine.progress * 44 / machine.maxProgress);
			drawTexturedModalRect(guiLeft + 73, guiTop + 63, 192, 14, p, 14);
		}

		fontRendererObj.drawString(machine.status, guiLeft + 54, guiTop + 86, 0xFFFFFF);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {

	}

	@Override
	protected void mouseClicked(int x, int y, int i) {
		super.mouseClicked(x, y, i);

		// Process drive
		if(checkClick(x, y, 52, 61, 18, 18)) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));

			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean("process", !machine.isProcessing);

			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, machine.xCoord, machine.yCoord, machine.zCoord));
		}

		// Clone drive
		if(checkClick(x, y, 53, 17, 18, 18)) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));

			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean("clone", true);

			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, machine.xCoord, machine.yCoord, machine.zCoord));
		}
	}

}
