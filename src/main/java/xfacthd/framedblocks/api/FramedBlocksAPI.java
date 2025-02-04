package xfacthd.framedblocks.api;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.api.util.WriteOnceHolder;

@SuppressWarnings("unused")
public interface FramedBlocksAPI
{
    WriteOnceHolder<FramedBlocksAPI> INSTANCE = new WriteOnceHolder<>();

    static FramedBlocksAPI getInstance() { return INSTANCE.get(); }



    String modid();

    /**
     * Returns the {@link BlockEntityType} used for all basic {@link xfacthd.framedblocks.api.block.AbstractFramedBlock}
     * implementations
     */
    BlockEntityType<FramedBlockEntity> defaultBlockEntity();

    /**
     * Returns the default {@link BlockState} used as a camo source when the block's camo state is set to air
     */
    BlockState defaultModelState();

    /**
     * Returns the {@link CreativeModeTab} that contains the FramedBlocks items
     */
    CreativeModeTab defaultCreativeTab();

    /**
     * Checks if the given {@link ItemStack} is a framed hammer
     */
    boolean isFramedHammer(ItemStack stack);

    /**
     * Checks if the give {@link FramedBlockEntity} is a double block (i.e. a Framed Double Slab)
     */
    boolean isFramedDoubleBlockEntity(FramedBlockEntity be);

    /**
     * Returns the current value of the {@code fireproofBlocks} setting in the common config
     */
    boolean areBlocksFireproof();

    /**
     * If true, all faces should be checked for interaction with neighboring blocks for culling purposes,
     * else only full faces should be checked against neighboring blocks
     */
    boolean detailedCullingEnabled();
}