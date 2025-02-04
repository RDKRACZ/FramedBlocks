package xfacthd.framedblocks.client.model;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.PropertyHolder;

public class FramedDoubleThreewayCornerModel extends FramedDoubleBlockModel
{
    private final BlockState state;

    public FramedDoubleThreewayCornerModel(BlockState state, BakedModel baseModel)
    {
        super(baseModel, true);
        this.state = state;
    }

    public FramedDoubleThreewayCornerModel(BakedModel baseModel)
    {
        this(
                FBContent.blockFramedDoubleThreewayCorner.get().defaultBlockState().setValue(PropertyHolder.FACING_HOR, Direction.WEST),
                baseModel
        );
    }

    @Override
    protected Tuple<BlockState, BlockState> getDummyStates()
    {
        Direction facing = state.getValue(PropertyHolder.FACING_HOR);
        boolean top = state.getValue(PropertyHolder.TOP);

        BlockState stateOne = FBContent.blockFramedInnerThreewayCorner.get().defaultBlockState()
                .setValue(PropertyHolder.TOP, top)
                .setValue(PropertyHolder.FACING_HOR, facing);
        BlockState stateTwo = FBContent.blockFramedThreewayCorner.get().defaultBlockState()
                .setValue(PropertyHolder.TOP, !top)
                .setValue(PropertyHolder.FACING_HOR, facing.getOpposite());

        return new Tuple<>(stateOne, stateTwo);
    }
}