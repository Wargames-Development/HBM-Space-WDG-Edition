package com.hbm.inventory.gui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerStationDriveTerminal;
import com.hbm.lib.RefStrings;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityMachineStationDriveTerminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIStationDriveTerminal extends GuiInfoContainer {

    private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_station_drive_terminal.png");
    private static final int CRT_X = 43;
    private static final int CRT_Y = 46;
    private static final int CRT_WIDTH = 56;
    private static final int ORB_X = 63;
    private static final int ORB_Y = 78;
    private static final int RAD_X = 87;
    private static final int RAD_Y = 78;
    private static final int MODE_W = 18;
    private static final int MODE_H = 18;

    private final TileEntityMachineStationDriveTerminal machine;

    public GUIStationDriveTerminal(InventoryPlayer player, TileEntityMachineStationDriveTerminal machine) {
        super(new ContainerStationDriveTerminal(player, machine));
        this.machine = machine;
        this.xSize = 176;
        this.ySize = 207;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 134, guiTop + 18, 16, 52, machine.power, machine.maxPower);
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 48, guiTop + 17, 19, 18, mouseX, mouseY, new String[] { "Clone drive" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 38, guiTop + 80, 19, 18, mouseX, mouseY, new String[] { "Start selected mode" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + ORB_X, guiTop + ORB_Y, MODE_W, MODE_H, mouseX, mouseY, new String[] { "Orbital station drive" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + RAD_X, guiTop + RAD_Y, MODE_W, MODE_H, mouseX, mouseY, new String[] { "Breach drive" });
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float interp, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int p = machine.maxPower <= 0L ? 0 : (int)(machine.power * 52L / machine.maxPower);
        p = Math.max(0, Math.min(52, p));
        drawTexturedModalRect(guiLeft + 134, guiTop + 18 + 52 - p, xSize, 52 - p, 16, p);

        boolean powered = machine.power >= machine.maxPower * 0.75D;
        if(!powered) {
            drawDisabled(ORB_X, ORB_Y, MODE_W, MODE_H);
            drawDisabled(RAD_X, RAD_Y, MODE_W, MODE_H);
        } else if(machine.selectedMode == TileEntityMachineStationDriveTerminal.MODE_ORBIT) {
            drawModeSelected(ORB_X, ORB_Y, MODE_W, MODE_H);
        } else if(machine.selectedMode == TileEntityMachineStationDriveTerminal.MODE_BREACH) {
            drawModeSelected(RAD_X, RAD_Y, MODE_W, MODE_H);
        }

        drawStatusText();
    }

    private void drawDisabled(int x, int y, int w, int h) {
        drawRect(guiLeft + x, guiTop + y, guiLeft + x + w, guiTop + y + h, 0x88000000);
    }

    private void drawModeSelected(int x, int y, int w, int h) {
        int left = guiLeft + x;
        int top = guiTop + y;
        drawRect(left, top, left + w, top + 1, 0xAAFFFFFF);
        drawRect(left, top + h - 1, left + w, top + h, 0xAAFFFFFF);
        drawRect(left, top, left + 1, top + h, 0xAAFFFFFF);
        drawRect(left + w - 1, top, left + w, top + h, 0xAAFFFFFF);
    }

    private void drawStatusText() {
        String text = machine.status == null ? "" : machine.status;
        List lines = fontRendererObj.listFormattedStringToWidth(text, CRT_WIDTH);
        int count = Math.min(3, lines.size());
        for(int i = 0; i < count; i++) {
            fontRendererObj.drawString((String)lines.get(i), guiLeft + CRT_X, guiTop + CRT_Y + i * 9, 0xFFFFFF);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) { }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if(checkClick(x, y, 48, 17, 19, 18)) send("clone");
        if(checkClick(x, y, 38, 80, 19, 18)) send("start");
        if(checkClick(x, y, ORB_X, ORB_Y, MODE_W, MODE_H)) send("orbit");
        if(checkClick(x, y, RAD_X, RAD_Y, MODE_W, MODE_H)) send("raid");
    }

    private void send(String action) {
        playButton();
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean(action, true);
        PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, machine.xCoord, machine.yCoord, machine.zCoord));
    }

    private void playButton() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }
}
