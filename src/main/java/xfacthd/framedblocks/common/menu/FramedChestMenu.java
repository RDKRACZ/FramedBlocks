package xfacthd.framedblocks.common.menu;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.FramedChestBlockEntity;

public class FramedChestMenu extends AbstractContainerMenu
{
    private final FramedChestBlockEntity chest;

    public FramedChestMenu(int windowId, Inventory inv, BlockEntity chest)
    {
        super(FBContent.menuTypeFramedChest.get(), windowId);

        Preconditions.checkArgument(chest instanceof FramedChestBlockEntity);
        this.chest = (FramedChestBlockEntity) chest;

        this.chest.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler ->
        {
            for (int row = 0; row < 3; ++row)
            {
                for (int col = 0; col < 9; ++col)
                {
                    addSlot(new SlotItemHandler(handler, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }
        });

        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            addSlot(new Slot(inv, col, 8 + col * 18, 143));
        }
    }

    public FramedChestMenu(int windowId, Inventory inv, FriendlyByteBuf extraData)
    {
        this(windowId, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    @Override
    public boolean stillValid(Player player) { return chest.isUsableByPlayer(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack remainder = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            remainder = stack.copy();
            if (index < 36)
            {
                if (!moveItemStackTo(stack, 36, slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!moveItemStackTo(stack, 0, 36, false))
            {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }

        return remainder;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);

        //noinspection ConstantConditions
        if (!chest.getLevel().isClientSide())
        {
            chest.close();
        }
    }
}