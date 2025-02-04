package xfacthd.framedblocks.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.data.FramedToolType;
import xfacthd.framedblocks.api.block.FramedBlockEntity;

import javax.annotation.Nullable;
import java.util.*;

public class FramedBlueprintItem extends FramedToolItem
{
    public static final String CONTAINED_BLOCK = "desc.framed_blocks:blueprint_block";
    public static final String CAMO_BLOCK = "desc.framed_blocks:blueprint_camo";
    public static final String IS_ILLUMINATED = "desc.framed_blocks:blueprint_illuminated";
    public static final MutableComponent BLOCK_NONE = new TranslatableComponent("desc.framed_blocks:blueprint_none").withStyle(ChatFormatting.RED);
    public static final MutableComponent BLOCK_INVALID = new TranslatableComponent("desc.framed_blocks:blueprint_invalid").withStyle(ChatFormatting.RED);
    public static final MutableComponent ILLUMINATED_FALSE = new TranslatableComponent("desc.framed_blocks:blueprint_illuminated_false").withStyle(ChatFormatting.RED);
    public static final MutableComponent ILLUMINATED_TRUE = new TranslatableComponent("desc.framed_blocks:blueprint_illuminated_true").withStyle(ChatFormatting.GREEN);
    public static final MutableComponent CANT_COPY = new TranslatableComponent("desc.framed_blocks:blueprint_cant_copy").withStyle(ChatFormatting.RED);

    public FramedBlueprintItem(FramedToolType type) { super(type); }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) { return false; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide())
            {
                CompoundTag tag = stack.getOrCreateTagElement("blueprint_data");
                tag.remove("framed_block");
                tag.remove("camo_data");
                tag.remove("camo_data_two");
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player == null) { return InteractionResult.FAIL; }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        CompoundTag tag = context.getItemInHand().getOrCreateTagElement("blueprint_data");

        if (player.isShiftKeyDown())
        {
            return writeBlueprint(level, pos, tag);
        }
        else if (!tag.isEmpty())
        {
            return readBlueprint(context, player, tag);
        }
        return super.useOn(context);
    }

    private static InteractionResult writeBlueprint(Level level, BlockPos pos, CompoundTag tag)
    {
        if (!(level.getBlockEntity(pos) instanceof FramedBlockEntity be))
        {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide())
        {
            BlockState state = level.getBlockState(pos);
            //noinspection ConstantConditions
            String block = state.getBlock().getRegistryName().toString();
            tag.putString("framed_block", block);

            CompoundTag nbt = be.writeToBlueprint();
            if (state.getBlock() == FBContent.blockFramedDoor.get())
            {
                boolean top = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                BlockPos posTwo = top ? pos.below() : pos.above();
                CompoundTag nbtTwo = level.getBlockEntity(posTwo) instanceof FramedBlockEntity beTwo ? beTwo.writeToBlueprint() : new CompoundTag();

                tag.put("camo_data", top ? nbtTwo : nbt);
                tag.put("camo_data_two", top ? nbt : nbtTwo);
            }
            else
            {
                tag.put("camo_data", nbt);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult readBlueprint(UseOnContext context, Player player, CompoundTag tag)
    {
        Block block = getTargetBlock(context.getItemInHand());

        if (block.defaultBlockState().isAir())
        {
            return InteractionResult.FAIL;
        }

        Item item = block.asItem();
        if (!(item instanceof BlockItem))
        {
            return InteractionResult.FAIL;
        }

        if (checkMissingMaterials(player, item, tag))
        {
            return InteractionResult.FAIL;
        }

        return tryPlace(context, player, item, tag);
    }

    private static boolean checkMissingMaterials(Player player, Item item, CompoundTag tag)
    {
        if (player.getAbilities().instabuild) { return false; } //Creative mode can always build

        CompoundTag camoData = tag.getCompound("camo_data");

        ItemStack camo = ItemStack.of(camoData.getCompound("camo_stack"));
        ItemStack camoTwo = camoData.contains("camo_stack_two") ? ItemStack.of(camoData.getCompound("camo_stack_two")) : ItemStack.EMPTY;
        boolean glowstone = tag.getCompound("camo_data").getBoolean("glowing");

        boolean doubleBlock = false;
        if (item == FBContent.blockFramedDoor.get().asItem() && tag.contains("camo_data_two"))
        {
            camoTwo = ItemStack.of(tag.getCompound("camo_data_two").getCompound("camo_stack"));
        }
        else if (item == FBContent.blockFramedDoublePanel.get().asItem())
        {
            item = FBContent.blockFramedPanel.get().asItem();
            doubleBlock = true;
        }
        else if (item == FBContent.blockFramedDoubleSlab.get().asItem())
        {
            item = FBContent.blockFramedSlab.get().asItem();
            doubleBlock = true;
        }



        if (doubleBlock)
        {
            int count = player.getInventory().countItem(item);
            if (count < 2) { return true; }
        }
        else
        {
            if (!player.getInventory().contains(new ItemStack(item))) { return true; }
        }

        if (!camo.isEmpty() && camo.is(camoTwo.getItem()))
        {
            int count = player.getInventory().countItem(camo.getItem());
            if (count < 2) { return true; }
        }
        else
        {
            if (!camo.isEmpty() && !player.getInventory().contains(camo)) { return true; }
            if (!camoTwo.isEmpty() && !player.getInventory().contains(camoTwo)) { return true; }
        }

        return glowstone && !player.getInventory().contains(Tags.Items.DUSTS_GLOWSTONE);
    }

    private InteractionResult tryPlace(UseOnContext context, Player player, Item item, CompoundTag tag)
    {
        ItemStack dummyStack = new ItemStack(item, 1);
        dummyStack.getOrCreateTag().put("BlockEntityTag", tag.getCompound("camo_data").copy());

        UseOnContext placeContext = new UseOnContext(
                context.getLevel(),
                context.getPlayer(),
                context.getHand(),
                dummyStack,
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside())
        );
        //Needs to happen before placing to make sure we really get the target pos, especially in case of replacing stuff like grass
        BlockPos topPos = new BlockPlaceContext(placeContext).getClickedPos().above();
        InteractionResult result = item.useOn(placeContext);

        if (!context.getLevel().isClientSide() && result.consumesAction())
        {
            if (item == FBContent.blockFramedDoor.get().asItem())
            {
                if (context.getLevel().getBlockEntity(topPos) instanceof FramedBlockEntity && tag.contains("camo_data_two", Constants.NBT.TAG_COMPOUND))
                {
                    //noinspection ConstantConditions
                    dummyStack.getOrCreateTag().put("BlockEntityTag", tag.get("camo_data_two"));
                    BlockItem.updateCustomBlockEntityTag(context.getLevel(), context.getPlayer(), topPos, dummyStack);
                }
            }

            if (!player.getAbilities().instabuild)
            {
                consumeItems(player, item, tag);
            }
        }

        return result;
    }

    private static void consumeItems(Player player, Item item, CompoundTag tag)
    {
        CompoundTag camoData = tag.getCompound("camo_data");

        ItemStack camo = ItemStack.of(camoData.getCompound("camo_stack"));
        ItemStack camoTwo = camoData.contains("camo_stack_two") ? ItemStack.of(camoData.getCompound("camo_stack_two")) : ItemStack.EMPTY;
        boolean glowstone = camoData.getBoolean("glowing");

        boolean doubleBlock = false;
        if (item == FBContent.blockFramedDoor.get().asItem() && tag.contains("camo_data_two"))
        {
            camoTwo = ItemStack.of(tag.getCompound("camo_data_two").getCompound("camo_stack"));
        }
        else if (item == FBContent.blockFramedDoublePanel.get().asItem())
        {
            item = FBContent.blockFramedPanel.get().asItem();
            doubleBlock = true;
        }
        else if (item == FBContent.blockFramedDoubleSlab.get().asItem())
        {
            item = FBContent.blockFramedSlab.get().asItem();
            doubleBlock = true;
        }

        int foundBlock = doubleBlock ? 2 : 1;
        boolean foundCamo = false;
        boolean foundCamoTwo = false;
        boolean foundGlowstone = false;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack stack = inv.getItem(i);
            if (foundBlock > 0 && stack.is(item))
            {
                int size = stack.getCount();
                stack.shrink(foundBlock);
                foundBlock -= size - stack.getCount();

                inv.setChanged();
            }

            if (!foundCamo && !camo.isEmpty() && stack.is(camo.getItem()))
            {
                foundCamo = true;

                stack.shrink(1);
                inv.setChanged();
            }
            if (!foundCamoTwo && !camoTwo.isEmpty() && stack.is(camoTwo.getItem()))
            {
                foundCamoTwo = true;

                stack.shrink(1);
                inv.setChanged();
            }

            if (!foundGlowstone && glowstone && stack.is(Tags.Items.DUSTS_GLOWSTONE))
            {
                foundGlowstone = true;

                stack.shrink(1);
                inv.setChanged();
            }

            if (foundBlock <= 0 && (camo.isEmpty() || foundCamo) && (camoTwo.isEmpty() || foundCamoTwo) && (!glowstone || foundGlowstone))
            {
                break;
            }
        }
    }

    public static Block getTargetBlock(ItemStack stack)
    {
        CompoundTag tag = stack.getOrCreateTagElement("blueprint_data");
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(tag.getString("framed_block")));
        Objects.requireNonNull(block);
        return block;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag)
    {
        CompoundTag tag = stack.getOrCreateTagElement("blueprint_data");
        if (tag.isEmpty())
        {
            components.add(new TranslatableComponent("desc.framed_blocks:blueprint_block", BLOCK_NONE).withStyle(ChatFormatting.GOLD));
        }
        else
        {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(tag.getString("framed_block")));
            Component blockName = block == null ? BLOCK_INVALID : block.getName().withStyle(ChatFormatting.WHITE);

            CompoundTag beTag = tag.getCompound("camo_data");
            Component camoName = !(block instanceof IFramedBlock fb) ? BLOCK_NONE : fb.printCamoBlock(beTag).orElse(BLOCK_NONE);
            Component illuminated = beTag.getBoolean("glowing") ? ILLUMINATED_TRUE : ILLUMINATED_FALSE;

            Component lineOne = new TranslatableComponent(CONTAINED_BLOCK, blockName).withStyle(ChatFormatting.GOLD);
            Component lineTwo = new TranslatableComponent(CAMO_BLOCK, camoName).withStyle(ChatFormatting.GOLD);
            Component lineThree = new TranslatableComponent(IS_ILLUMINATED, illuminated).withStyle(ChatFormatting.GOLD);

            components.addAll(Arrays.asList(lineOne, lineTwo, lineThree));
        }
    }
}