package si.meansoft.traincraft.items;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import si.meansoft.traincraft.api.ITraincraftTrack;
import si.meansoft.traincraft.tile.TileEntityTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author canitzp
 */
public class ItemBlockTrack extends ItemBlockBase {

    public ItemBlockTrack(Block block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (this.block instanceof ITraincraftTrack) {
            pos = pos.up();
            ITraincraftTrack track = (ITraincraftTrack) this.block;
            EnumFacing horizontalFacing = playerIn.getHorizontalFacing();
            boolean flipAlongX = track.getTrackType().isCurve() && faceLeft(horizontalFacing, hitX, hitZ);
            if (track.canPlaceTrack(worldIn, pos, playerIn, stack, hitX, hitY, hitZ, flipAlongX)) {
                if (stack.stackSize > 0 && playerIn.canPlayerEdit(pos, facing, stack) && worldIn.canBlockBePlaced(this.block, pos, false, facing, playerIn, stack)) {
                    IBlockState state = this.block.onBlockPlaced(worldIn, pos, facing, hitX, hitY, hitZ, this.getMetadata(stack), playerIn);
                    if (state != null) {
                        List<BlockPos> settedBlocks = track.getPositionToPlace(worldIn, pos, playerIn, hitX, hitY, hitZ, flipAlongX);
                        int blockIndex = 0;
                        if(playerIn.isCreative() || stack.stackSize >= settedBlocks.size()){
                            for (BlockPos pos1 : settedBlocks) {
                                if (placeBlockAt(stack, playerIn, worldIn, pos1, facing, hitX, hitY, hitZ, state)) {
                                    SoundType soundtype = worldIn.getBlockState(pos1).getBlock().getSoundType(worldIn.getBlockState(pos1), worldIn, pos1, playerIn);
                                    worldIn.playSound(playerIn, pos1, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                                    stack.stackSize--;
                                    if (pos1 != pos) {
                                        TileEntityTrack tile = (TileEntityTrack) worldIn.getTileEntity(pos1);
                                        if (tile != null) {
                                            tile.create(pos, blockIndex, horizontalFacing);
                                        }
                                    } else {
                                        List<BlockPos> toDestroy = new ArrayList<>(settedBlocks);
                                        toDestroy.remove(pos);
                                        TileEntityTrack tile = (TileEntityTrack) worldIn.getTileEntity(pos1);
                                        if (tile != null) {
                                            tile.create(toDestroy, blockIndex, flipAlongX, horizontalFacing);
                                        }
                                    }
                                    blockIndex++;
                                }
                            }
                        }
                    }
                }
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    protected boolean faceLeft(EnumFacing facing, float hitX, float hitZ) {
        switch (facing) {
            case NORTH:
                return hitX < 0.5;
            case EAST:
                return hitZ < 0.5;
            case SOUTH:
                return hitX > 0.5;
            case WEST:
                return hitZ > 0.5;
        }
        return false;
    }

}
