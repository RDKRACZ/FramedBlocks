package xfacthd.framedblocks.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.api.util.Utils;

public class FramedDoublePanelBlockEntity extends FramedDoubleBlockEntity
{
    public FramedDoublePanelBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.blockEntityTypeDoubleFramedPanel.get(), pos, state);
    }

    @Override
    protected boolean hitSecondary(BlockHitResult hit)
    {
        Direction facing = getBlockState().getValue(PropertyHolder.FACING_NE);
        Direction side = hit.getDirection();
        Vec3 vec = Utils.fraction(hit.getLocation());

        if (side == facing) { return false; }
        if (side == facing.getOpposite()) { return true; }

        if (facing == Direction.NORTH)
        {
            return vec.z() > .5F;
        }
        else
        {
            return vec.x() <= .5F;
        }
    }

    @Override
    public BlockState getCamoState(Direction side)
    {
        Direction facing = getBlockState().getValue(PropertyHolder.FACING_NE);
        if (side == facing) { return getCamoState(); }
        if (side == facing.getOpposite()) { return getCamoStateTwo(); }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSolidSide(Direction side)
    {
        Direction facing = getBlockState().getValue(PropertyHolder.FACING_NE);
        if (side == facing || side == facing.getOpposite())
        {
            //noinspection ConstantConditions
            return getCamoState(side).isSolidRender(level, worldPosition);
        }
        //noinspection ConstantConditions
        return getCamoState().isSolidRender(level, worldPosition) && getCamoStateTwo().isSolidRender(level, worldPosition);
    }
}