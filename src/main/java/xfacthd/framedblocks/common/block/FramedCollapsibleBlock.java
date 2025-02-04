package xfacthd.framedblocks.common.block;

import com.google.common.cache.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.*;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.CtmPredicate;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.FramedCollapsibleBlockEntity;
import xfacthd.framedblocks.common.data.*;

public class FramedCollapsibleBlock extends FramedBlock
{
    public static final CtmPredicate CTM_PREDICATE = (state, dir) ->
    {
        CollapseFace face = state.getValue(PropertyHolder.COLLAPSED_FACE);
        if (face == CollapseFace.NONE) { return true; }
        return dir == face.toDirection().getOpposite();
    };

    private static final LoadingCache<Integer, VoxelShape> SHAPE_CACHE = CacheBuilder.newBuilder().maximumSize(1024).build(new ShapeLoader());

    public FramedCollapsibleBlock(BlockType blockType)
    {
        super(blockType, IFramedBlock.createProperties().dynamicShape());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(PropertyHolder.COLLAPSED_FACE);
    }

    public static boolean onLeftClick(Level level, BlockPos pos, Player player)
    {
        if (player.getMainHandItem().getItem() != FBContent.itemFramedHammer.get()) { return false; }

        if (level.getBlockEntity(pos) instanceof FramedCollapsibleBlockEntity be)
        {
            if (!level.isClientSide())
            {
                be.handleDeform(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        CollapseFace face = state.getValue(PropertyHolder.COLLAPSED_FACE);
        if (face != CollapseFace.NONE)
        {
            if (level.getBlockEntity(pos) instanceof FramedCollapsibleBlockEntity be)
            {
                int offsets = be.getPackedOffsets();
                offsets |= (face.toDirection().get3DDataValue() << 20);
                return SHAPE_CACHE.getUnchecked(offsets);
            }
        }
        return Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedCollapsibleBlockEntity(pos, state);
    }



    private static class ShapeLoader extends CacheLoader<Integer, VoxelShape>
    {
        @Override
        public VoxelShape load(Integer packedData)
        {
            Direction face = Direction.from3DDataValue(packedData >> 20);
            byte[] offsets = FramedCollapsibleBlockEntity.unpackOffsets(packedData & 0xFFFFF);

            boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            boolean flipX = face == Direction.NORTH || face == Direction.EAST;
            boolean flipZ = face != Direction.UP;

            VoxelShape result = Shapes.empty();
            for (int x = 0; x < 4; x++)
            {
                for (int z = 0; z < 4; z++)
                {
                    double x0 = flipX ? (1D - x / 4D) : (x / 4D);
                    double x1 = flipX ? (1D - (x + 1) / 4D) : ((x + 1) / 4D);
                    double z0 = flipZ ? (1D - z / 4D) : (z / 4D);
                    double z1 = flipZ ? (1D - (z + 1) / 4D) : ((z + 1) / 4D);

                    double y0 = Mth.lerp2(x0, z0, offsets[0], offsets[3], offsets[1], offsets[2]);
                    double y1 = Mth.lerp2(x1, z1, offsets[0], offsets[3], offsets[1], offsets[2]);

                    double y = positive ?
                            Math.max(16D - Math.min(y0, y1), Mth.EPSILON * 2D) :
                            Math.min(Math.min(y0, y1), 16D - (Mth.EPSILON * 2D));

                    VoxelShape shape = switch (face)
                            {
                                case NORTH -> box(x * 4, z * 4, y, (x + 1) * 4, (z + 1) * 4, 16);
                                case EAST -> box(0, z * 4, x * 4, y, (z + 1) * 4, (x + 1) * 4);
                                case SOUTH -> box(x * 4, z * 4, 0, (x + 1) * 4, (z + 1) * 4, y);
                                case WEST -> box(y, z * 4, x * 4, 16, (z + 1) * 4, (x + 1) * 4);
                                case UP -> box(x * 4, 0, z * 4, (x + 1) * 4, y, (z + 1) * 4);
                                case DOWN -> box(x * 4, y, z * 4, (x + 1) * 4, 16, (z + 1) * 4);
                            };

                    result = Shapes.join(result, shape, BooleanOp.OR);
                }
            }

            result = result.optimize();

            return result;
        }
    }
}