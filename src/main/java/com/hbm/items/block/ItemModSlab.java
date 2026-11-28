package com.hbm.items.block;

import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMultiSlab;
import com.hbm.interfaces.NotableComments;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;

@NotableComments
public class ItemModSlab extends ItemSlab {

	public ItemModSlab(Block block) {
		super(block, shittyFuckingHackSingle(block), shittyFuckingHackDouble(block), shittyFuckingHackDouble(block) == block);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
		super.addInformation(stack, player, list, advanced);
		BlockMultiSlab slab = (BlockMultiSlab) field_150939_a;
		ItemBlockBlastInfo.addBlastResistance(list, slab.getExplosionResistance(null, stack.getItemDamage()));
	}
	
	public static BlockSlab shittyFuckingHackSingle(Block b) {
		if(b == ModBlocks.concrete_slab || b == ModBlocks.concrete_double_slab) return (BlockSlab) ModBlocks.concrete_slab;
		if(b == ModBlocks.concrete_brick_slab || b == ModBlocks.concrete_brick_double_slab) return (BlockSlab) ModBlocks.concrete_brick_slab;
		if(b == ModBlocks.brick_slab || b == ModBlocks.brick_double_slab) return (BlockSlab) ModBlocks.brick_slab;
		if(b == ModBlocks.stones_slab || b == ModBlocks.stones_double_slab) return (BlockSlab) ModBlocks.stones_slab;
		return null;
	}
	
	public static BlockSlab shittyFuckingHackDouble(Block b) {
		if(b == ModBlocks.concrete_slab || b == ModBlocks.concrete_double_slab) return (BlockSlab) ModBlocks.concrete_double_slab;
		if(b == ModBlocks.concrete_brick_slab || b == ModBlocks.concrete_brick_double_slab) return (BlockSlab) ModBlocks.concrete_brick_double_slab;
		if(b == ModBlocks.brick_slab || b == ModBlocks.brick_double_slab) return (BlockSlab) ModBlocks.brick_double_slab;
		if(b == ModBlocks.stones_slab || b == ModBlocks.stones_double_slab) return (BlockSlab) ModBlocks.stones_double_slab;
		return null;
	}
}
