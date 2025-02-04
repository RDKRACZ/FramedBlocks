package xfacthd.framedblocks.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import xfacthd.framedblocks.api.model.BakedModelProxy;
import xfacthd.framedblocks.api.util.FramedBlockData;
import xfacthd.framedblocks.api.util.client.ModelUtils;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class FramedDoubleBlockModel extends BakedModelProxy
{
    private final boolean specialItemModel;
    private Tuple<BlockState, BlockState> dummyStates = null;
    private Tuple<BakedModel, BakedModel> models = null;

    protected FramedDoubleBlockModel(BakedModel baseModel, boolean specialItemModel)
    {
        super(baseModel);
        this.specialItemModel = specialItemModel;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData)
    {
        if (dummyStates == null) { dummyStates = getDummyStates(); }
        if (models == null) { models = getModels(); }

        IModelData dataLeft = extraData.getData(FramedDoubleBlockEntity.DATA_LEFT);
        List<BakedQuad> quads = new ArrayList<>(
                models.getA().getQuads(dummyStates.getA(), side, rand, dataLeft != null ? dataLeft : EmptyModelData.INSTANCE)
        );

        IModelData dataRight = extraData.getData(FramedDoubleBlockEntity.DATA_RIGHT);
        quads.addAll(invertTintIndizes(
                models.getB().getQuads(dummyStates.getB(), side, rand, dataRight != null ? dataRight : EmptyModelData.INSTANCE)
        ));

        return quads;
    }

    @Override
    @Deprecated
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand)
    {
        if (specialItemModel)
        {
            if (dummyStates == null) { dummyStates = getDummyStates(); }
            if (models == null) { models = getModels(); }

            List<BakedQuad> quads = new ArrayList<>(models.getA().getQuads(state, side, rand));
            quads.addAll(models.getB().getQuads(state, side, rand));
            return quads;
        }
        return super.getQuads(state, side, rand);
    }

    @Override
    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public TextureAtlasSprite getParticleIcon(@Nonnull IModelData data)
    {
        IModelData innerData = data.getData(FramedDoubleBlockEntity.DATA_LEFT);
        if (innerData != null && !innerData.getData(FramedBlockData.CAMO).isAir())
        {
            return models.getA().getParticleIcon(innerData);
        }
        innerData = data.getData(FramedDoubleBlockEntity.DATA_RIGHT);
        if (innerData != null && !innerData.getData(FramedBlockData.CAMO).isAir())
        {
            return models.getB().getParticleIcon(innerData);
        }
        return baseModel.getParticleIcon();
    }

    @Nonnull
    @Override
    public IModelData getModelData(@Nonnull BlockAndTintGetter world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData)
    {
        if (world.getBlockEntity(pos) instanceof FramedDoubleBlockEntity be)
        {
            return be.getModelData();
        }
        return tileData;
    }



    protected abstract Tuple<BlockState, BlockState> getDummyStates();

    private Tuple<BakedModel, BakedModel> getModels()
    {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        return new Tuple<>(
                dispatcher.getBlockModel(dummyStates.getA()),
                dispatcher.getBlockModel(dummyStates.getB())
        );
    }

    private static List<BakedQuad> invertTintIndizes(List<BakedQuad> quads)
    {
        return quads.stream()
                .map(ModelUtils::invertTintIndex)
                .collect(Collectors.toList());
    }
}