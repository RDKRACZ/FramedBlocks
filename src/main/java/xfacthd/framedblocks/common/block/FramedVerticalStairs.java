package xfacthd.framedblocks.common.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.*;
import xfacthd.framedblocks.api.util.*;
import xfacthd.framedblocks.common.data.*;

import java.util.stream.Stream;

public class FramedVerticalStairs extends FramedBlock
{
    public static final CtmPredicate CTM_PREDICATE = (state, side) ->
    {
        StairsType type = state.getValue(PropertyHolder.STAIRS_TYPE);
        if (type == StairsType.VERTICAL)
        {
            Direction dir = state.getValue(PropertyHolder.FACING_HOR);
            return side == dir || side == dir.getCounterClockWise();
        }
        return false;
    };

    public FramedVerticalStairs() { super(BlockType.FRAMED_VERTICAL_STAIRS); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(PropertyHolder.FACING_HOR, PropertyHolder.STAIRS_TYPE, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = defaultBlockState().setValue(PropertyHolder.FACING_HOR, context.getHorizontalDirection());
        return getStateFromContext(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos)
    {
        Direction dir = state.getValue(PropertyHolder.FACING_HOR);
        if (facing == dir.getOpposite() || facing == dir.getClockWise()) { return state; }

        return getStateFromContext(state, level, pos);
    }

    private BlockState getStateFromContext(BlockState state, LevelAccessor level, BlockPos pos)
    {
        Direction dir = state.getValue(PropertyHolder.FACING_HOR);

        BlockState front = level.getBlockState(pos.relative(dir));
        BlockState left = level.getBlockState(pos.relative(dir.getCounterClockWise()));

        if (!(front.getBlock() instanceof StairBlock) && !(left.getBlock() instanceof StairBlock))
        {
            return state.setValue(PropertyHolder.STAIRS_TYPE, StairsType.VERTICAL);
        }
        else
        {
            StairsType type;

            boolean topCorner = false;
            boolean bottomCorner = false;

            if (front.getBlock() instanceof StairBlock && front.getValue(BlockStateProperties.HORIZONTAL_FACING) == dir.getCounterClockWise())
            {
                topCorner = front.getValue(BlockStateProperties.HALF) == Half.BOTTOM;
                bottomCorner = front.getValue(BlockStateProperties.HALF) == Half.TOP;
            }

            if (left.getBlock() instanceof StairBlock && left.getValue(BlockStateProperties.HORIZONTAL_FACING) == dir)
            {
                topCorner |= left.getValue(BlockStateProperties.HALF) == Half.BOTTOM;
                bottomCorner |= left.getValue(BlockStateProperties.HALF) == Half.TOP;
            }

            BlockState above = level.getBlockState(pos.above());
            BlockState below = level.getBlockState(pos.below());

            if (topCorner && !above.is(this)) { type = StairsType.TOP_CORNER; }
            else if (bottomCorner && !below.is(this)) { type = StairsType.BOTTOM_CORNER; }
            else { type = StairsType.VERTICAL; }

            return state.setValue(PropertyHolder.STAIRS_TYPE, type);
        }
    }

    public static ImmutableMap<BlockState, VoxelShape> generateShapes(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        VoxelShape vertShape = Shapes.join(
                Block.box(0, 0, 8, 16, 16, 16),
                Block.box(8, 0, 0, 16, 16, 8),
                BooleanOp.OR
        );

        VoxelShape topCornerShape = Stream.of(
                Block.box(8, 0, 8, 16, 16, 16),
                Block.box(8, 0, 0, 16, 8, 8),
                Block.box(0, 0, 8, 8, 8, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

        VoxelShape bottomCornerShape = Stream.of(
                Block.box(8, 0, 8, 16, 16, 16),
                Block.box(8, 8, 0, 16, 16, 8),
                Block.box(0, 8, 8, 8, 16, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

        for (BlockState state : states)
        {
            StairsType type = state.getValue(PropertyHolder.STAIRS_TYPE);
            Direction dir = state.getValue(PropertyHolder.FACING_HOR).getOpposite();

            if (type == StairsType.TOP_CORNER)
            {
                builder.put(state, Utils.rotateShape(Direction.NORTH, dir, topCornerShape));
            }
            else if (type == StairsType.BOTTOM_CORNER)
            {
                builder.put(state, Utils.rotateShape(Direction.NORTH, dir, bottomCornerShape));
            }
            else
            {
                builder.put(state, Utils.rotateShape(Direction.NORTH, dir, vertShape));
            }
        }

        return builder.build();
    }
}