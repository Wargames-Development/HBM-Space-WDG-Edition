package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineStationDriveTerminal;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Single-block powered terminal for orbital and Breach drive programming. */
public class MachineStationDriveTerminal extends BlockMachineBase {

    public MachineStationDriveTerminal(Material material) {
        super(material, 0);
        this.rotatable = true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityMachineStationDriveTerminal();
    }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public int getRenderType() { return -1; }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = register.registerIcon("hbm:block_steel_machine");
    }
}
