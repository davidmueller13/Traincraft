package si.meansoft.traincraft.api;

import mods.railcraft.common.carts.IRailcraftCartContainer;
import mods.railcraft.common.carts.ItemCart;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import si.meansoft.traincraft.Compat;
import si.meansoft.traincraft.IRegistryEntry;
import si.meansoft.traincraft.Traincraft;
import si.meansoft.traincraft.blocks.BlockContainerBase;
import si.meansoft.traincraft.compat.RailcraftUtil;
import si.meansoft.traincraft.items.ItemBlockBase;
import si.meansoft.traincraft.tile.TileEntityTrack;
import si.meansoft.traincraft.track.TrackType;

import java.util.Arrays;
import java.util.List;

/**
 * @author canitzp
 */
public abstract class AbstractBlockTrack extends BlockContainerBase implements ITraincraftTrack{

    private TrackType type;

    protected static final AxisAlignedBB FLAT_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);

    public AbstractBlockTrack(TrackType type, Class<? extends TileEntityTrack> tileClass){
        super(Material.IRON, "track" + type.getInternName(), tileClass);
        this.setCreativeTab(Traincraft.trackTab);
        this.type = type;
    }

    @Override
    public IRegistryEntry[] getRegisterElements() {
        return new IRegistryEntry[]{this, getItemBlock(this), createNewTileEntity(null, 0)};
    }

    protected abstract ItemBlockBase getItemBlock(AbstractBlockTrack track);

    @Override
    public TrackType getTrackType(){
        return this.type;
    }

    @Override
    public boolean canPlaceTrack(World world, BlockPos pos, EntityLivingBase placer, ItemStack stack, float hitX, float hitY, float hitZ, boolean flipAlongX){
        EnumFacing dir = placer.getHorizontalFacing();
        for (BlockPos pos1 : getTrackType().getGrid().getPosesToAffect(pos, dir, flipAlongX)) {
            if (!world.getBlockState(pos1).getBlock().canReplace(world, pos1, dir, stack) || !world.isSideSolid(pos1.down(), EnumFacing.UP)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<BlockPos> getPositionToPlace(World world, BlockPos pos, EntityLivingBase placer, float hitX, float hitY, float hitZ, boolean flipAlongX){
        EnumFacing dir = placer.getHorizontalFacing();
        return getTrackType().getGrid().getPosesToAffect(pos, dir, getTrackType().isCurve() && flipAlongX);
    }

    private boolean flag = false;

    @Override
    public void onMinecartDriveOver(World world, BlockPos pos, IBlockState state, EntityMinecart cart, Entity ridingEntity){
        EnumFacing facing = state.getValue(FACING);
        BlockPos nextPos = pos.offset(facing.getOpposite());
        float rot = ridingEntity != null ? ridingEntity.getRotationYawHead() + 90 : cart.rotationYaw;
        System.out.println(cart.rotationYaw);
        cart.rotationYaw = 90;
        if(!flag && world.getBlockState(nextPos).getBlock() instanceof ITraincraftTrack){
            //cart.moveToBlockPosAndAngles(pos.offset(facing.getOpposite()), rot, cart.rotationPitch);
            flag = false;
        } else flag = true;
        nextPos = pos.offset(facing);
        if(flag && world.getBlockState(nextPos).getBlock() instanceof ITraincraftTrack){
            //cart.moveToBlockPosAndAngles(pos.offset(facing), rot, cart.rotationPitch);
        } else flag = false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return FLAT_AABB;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos pos){return FLAT_AABB;}

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        this.removeTrack(world, pos, !player.isCreative());
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        this.removeTrack(world, pos, world.rand.nextInt(3) == 0);
        super.onBlockExploded(world, pos, explosion);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack stack, EnumFacing side, float hitX, float hitY, float hitZ){
        if(stack != null){
            if(Compat.isRailcraftLoaded && stack.getItem() instanceof ItemCart){
                if(!world.isRemote){
                    processRailcraftItem(stack, player, pos);
                }
                if(!player.isCreative()) stack.stackSize--;
                return true;
            }
            if(stack.getItem() instanceof ItemMinecart){
                if(!world.isRemote){
                    EntityMinecart.Type minecartType = ReflectionHelper.getPrivateValue(ItemMinecart.class, (ItemMinecart) stack.getItem(), 1);
                    System.out.println(Arrays.toString(EntityMinecart.Type.values()));
                    EntityMinecart cart = EntityMinecart.create(world, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.0625D, (double) pos.getZ() + 0.5D, minecartType);
                    if(stack.hasDisplayName()){
                        cart.setCustomNameTag(stack.getDisplayName());
                    }
                    world.spawnEntityInWorld(cart);
                }
                if(!player.isCreative()) stack.stackSize--;
                return true;
            }
        }
        return super.onBlockActivated(world, pos, state, player, hand, stack, side, hitX, hitY, hitZ);
    }

    @Optional.Method(modid = "railcraft")
    private void processRailcraftItem(ItemStack stack, EntityPlayer player, BlockPos pos){
        RailcraftUtil.placeRailcraftCart(((ItemCart)stack.getItem()).getCartType(), player, stack, player.getEntityWorld(), pos);
    }
}
