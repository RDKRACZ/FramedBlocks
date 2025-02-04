package xfacthd.framedblocks.common.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.common.data.BlockType;

import javax.annotation.Nullable;

public class FramedHalfPillarBlock extends FramedBlock
{
    public FramedHalfPillarBlock(BlockType blockType) { super(blockType); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = defaultBlockState();
        state = state.setValue(BlockStateProperties.FACING, context.getClickedFace().getOpposite());
        return withWater(state, context.getLevel(), context.getClickedPos());
    }

    public static ImmutableMap<BlockState, VoxelShape> generateShapes(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        VoxelShape shapeNorth = box(4, 4, 0, 12, 12,  8);
        VoxelShape shapeSouth = box(4, 4, 8, 12, 12, 16);
        VoxelShape shapeEast =  box(8, 4, 4, 16, 12, 12);
        VoxelShape shapeWest =  box(0, 4, 4,  8, 12, 12);
        VoxelShape shapeUp =    box(4, 8, 4, 12, 16, 12);
        VoxelShape shapeDown =  box(4, 0, 4, 12,  8, 12);

        for (BlockState state : states)
        {
            builder.put(state, switch (state.getValue(BlockStateProperties.FACING))
            {
                case NORTH -> shapeNorth;
                case EAST -> shapeEast;
                case SOUTH -> shapeSouth;
                case WEST -> shapeWest;
                case UP -> shapeUp;
                case DOWN -> shapeDown;
            });
        }

        return builder.build();
    }
}