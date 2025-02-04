package xfacthd.framedblocks.client.model;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.PropertyHolder;

public class FramedDoublePrismCornerModel extends FramedDoubleBlockModel
{
    private final BlockState state;

    public FramedDoublePrismCornerModel(BlockState state, BakedModel baseModel)
    {
        super(baseModel, true);
        this.state = state;
    }

    public FramedDoublePrismCornerModel(BakedModel baseModel)
    {
        this(
                FBContent.blockFramedDoublePrismCorner.get().defaultBlockState().setValue(PropertyHolder.FACING_HOR, Direction.WEST),
                baseModel
        );
    }

    @Override
    protected Tuple<BlockState, BlockState> getDummyStates()
    {
        Direction facing = state.getValue(PropertyHolder.FACING_HOR);
        boolean top = state.getValue(PropertyHolder.TOP);
        boolean offset = state.getValue(PropertyHolder.OFFSET);

        BlockState stateOne = FBContent.blockFramedInnerPrismCorner.get().defaultBlockState()
                .setValue(PropertyHolder.TOP, top)
                .setValue(PropertyHolder.FACING_HOR, facing)
                .setValue(PropertyHolder.OFFSET, offset);
        BlockState stateTwo = FBContent.blockFramedPrismCorner.get().defaultBlockState()
                .setValue(PropertyHolder.TOP, !top)
                .setValue(PropertyHolder.FACING_HOR, facing.getOpposite())
                .setValue(PropertyHolder.OFFSET, !offset);

        return new Tuple<>(stateOne, stateTwo);
    }
}