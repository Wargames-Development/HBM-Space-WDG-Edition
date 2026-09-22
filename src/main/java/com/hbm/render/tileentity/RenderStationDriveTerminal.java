package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachineStationDriveTerminal;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderStationDriveTerminal extends TileEntitySpecialRenderer implements IItemRendererProvider {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        if(!(te instanceof TileEntityMachineStationDriveTerminal)) return;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // The authored OBJ faces -Z. Metadata 2 is NORTH, so it requires no base rotation.
        switch(te.getBlockMetadata()) {
        case 2: break;
        case 5: GL11.glRotatef(270F, 0F, 1F, 0F); break;
        case 3: GL11.glRotatef(180F, 0F, 1F, 0F); break;
        case 4: GL11.glRotatef(90F, 0F, 1F, 0F); break;
        default: break;
        }

        bindTexture(ResourceManager.station_drive_terminal_tex);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_CULL_FACE);
        ResourceManager.station_drive_terminal.renderAll();
        if(cullEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopMatrix();
    }

    @Override
    public IItemRenderer getRenderer() {
        return new ItemRenderBase() {
            public void renderInventory() {
                GL11.glTranslated(0D, -2.0D, 0D);
                GL11.glScaled(8.0D, 8.0D, 8.0D);
                // renderCommon() keeps the legacy half-block transform used by held/entity
                // renders. Cancel it only for inventory so the correctly-scaled icon is centered.
                GL11.glTranslated(-0.5D, 0D, -0.5D);
            }
            public void renderCommon() {
                GL11.glTranslated(0.5D, 0D, 0.5D);
                GL11.glShadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.station_drive_terminal_tex);
                boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_CULL_FACE);
                ResourceManager.station_drive_terminal.renderAll();
                if(cullEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glShadeModel(GL11.GL_FLAT);
            }
        };
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_station_drive_terminal);
    }
}
