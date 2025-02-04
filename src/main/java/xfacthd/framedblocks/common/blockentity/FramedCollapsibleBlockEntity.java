package xfacthd.framedblocks.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.model.data.ModelProperty;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.CollapseFace;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.util.MathUtils;

import java.util.Arrays;

public class FramedCollapsibleBlockEntity extends FramedBlockEntity
{
    public static final ModelProperty<Integer> OFFSETS = new ModelProperty<>();
    public static final ModelProperty<Direction> COLLAPSED_FACE = new ModelProperty<>();

    private Direction collapsedFace = null;
    private byte[] vertexOffsets = new byte[4];

    public FramedCollapsibleBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.blockEntityTypeFramedCollapsibleBlock.get(), pos, state);
    }

    public void handleDeform(Player player)
    {
        HitResult hit = player.pick(10D, 0, false);
        if (!(hit instanceof BlockHitResult blockHit)) { return; }

        Direction faceHit = blockHit.getDirection();
        Vec3 hitLoc = Utils.fraction(hit.getLocation());

        if (collapsedFace != null && faceHit != collapsedFace) { return; }

        int vert = vertexFromHit(faceHit, hitLoc);
        if (player.isShiftKeyDown() && collapsedFace != null && vertexOffsets[vert] > 0)
        {
            byte target = (byte) (vertexOffsets[vert] - 1);

            applyDeformation(vert, target, faceHit);
            deformNeighbors(faceHit, hitLoc, target);
        }
        else if (!player.isShiftKeyDown() && vertexOffsets[vert] < 16)
        {
            byte target = (byte) (vertexOffsets[vert] + 1);

            applyDeformation(vert, target, faceHit);
            deformNeighbors(faceHit, hitLoc, target);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void applyDeformation(int vertex, byte offset, Direction faceHit)
    {
        offset = Mth.clamp(offset, (byte) 0, (byte) 16);

        if (offset == vertexOffsets[vertex]) { return; }

        vertexOffsets[vertex] = offset;

        if (offset == 0)
        {
            boolean noOffsets = true;
            for (int i = 0; i < 4; i++)
            {
                if (vertexOffsets[i] > 0)
                {
                    noOffsets = false;
                    break;
                }
            }

            if (noOffsets)
            {
                collapsedFace = null;
                level.setBlock(worldPosition, getBlockState().setValue(PropertyHolder.COLLAPSED_FACE, CollapseFace.NONE), Block.UPDATE_ALL);
            }
            else
            {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        else if (collapsedFace == null)
        {
            collapsedFace = faceHit;
            level.setBlock(worldPosition, getBlockState().setValue(PropertyHolder.COLLAPSED_FACE, CollapseFace.fromDirection(collapsedFace)), Block.UPDATE_ALL);
        }
        else
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        setChanged();
    }

    private void deformNeighbors(Direction faceHit, Vec3 hitLoc, byte offset)
    {
        BlockPos[] neighbors = new BlockPos[3];
        Vec3[] hitVecs = new Vec3[3];
        if (faceHit.getAxis() == Direction.Axis.Y)
        {
            Direction dirX = hitLoc.x > .5 ? Direction.EAST : Direction.WEST;
            Direction dirZ = hitLoc.z > .5 ? Direction.SOUTH : Direction.NORTH;

            neighbors[0] = worldPosition.relative(dirX);
            neighbors[1] = worldPosition.relative(dirZ);
            neighbors[2] = neighbors[0].relative(dirZ);

            hitVecs[0] = hitLoc.add(dirX.getStepX() * .5, 0, 0);
            hitVecs[1] = hitLoc.add(0, 0, dirZ.getStepZ() * .5);
            hitVecs[2] = hitVecs[0].add(0, 0, dirZ.getStepZ() * .5);
        }
        else
        {
            Direction dirY = hitLoc.y > .5 ? Direction.UP : Direction.DOWN;
            Direction dirXZ;

            if (faceHit.getAxis() == Direction.Axis.X)
            {
                dirXZ = hitLoc.z > .5 ? Direction.SOUTH : Direction.NORTH;
            }
            else
            {
                dirXZ = hitLoc.x > .5 ? Direction.EAST : Direction.WEST;
            }

            neighbors[0] = worldPosition.relative(dirY);
            neighbors[1] = worldPosition.relative(dirXZ);
            neighbors[2] = neighbors[0].relative(dirXZ);

            hitVecs[0] = hitLoc.add(0, dirY.getStepY() * .5, 0);
            hitVecs[1] = hitLoc.add(dirXZ.getStepX() * .5, 0, dirXZ.getStepZ() * .5);
            hitVecs[2] = hitVecs[0].add(dirXZ.getStepX() * .5, 0, dirXZ.getStepZ() * .5);
        }

        for (int i = 0; i < 3; i++)
        {
            //noinspection ConstantConditions
            if (level.getBlockEntity(neighbors[i]) instanceof FramedCollapsibleBlockEntity be)
            {
                int vert = vertexFromHit(faceHit, MathUtils.wrapVector(hitVecs[i], 0, 1));
                be.applyDeformation(vert, offset, faceHit);
            }
        }
    }

    private static int vertexFromHit(Direction faceHit, Vec3 loc)
    {
        if (faceHit.getAxis() == Direction.Axis.Y)
        {
            if ((loc.z < .5F) == (faceHit == Direction.UP))
            {
                return loc.x < .5F ? 0 : 3;
            }
            else
            {
                return loc.x < .5F ? 1 : 2;
            }
        }
        else
        {
            boolean positive = faceHit == Direction.SOUTH || faceHit == Direction.WEST;
            double xz = faceHit.getAxis() == Direction.Axis.X ? loc.z : loc.x;
            if (loc.y < .5F)
            {
                return (xz < .5F) == positive ? 1 : 2;
            }
            else
            {
                return (xz < .5F) == positive ? 0 : 3;
            }
        }
    }

    public byte[] getVertexOffsets() { return vertexOffsets; }

    public int getPackedOffsets() { return packOffsets(vertexOffsets); }

    @Override
    protected void writeToDataPacket(CompoundTag nbt)
    {
        super.writeToDataPacket(nbt);
        nbt.putInt("offsets", packOffsets(vertexOffsets));
        nbt.putInt("face", collapsedFace == null ? -1 : collapsedFace.get3DDataValue());
    }

    @Override
    protected boolean readFromDataPacket(CompoundTag nbt)
    {
        boolean needUpdate = super.readFromDataPacket(nbt);

        int packed = nbt.getInt("offsets");
        byte[] offsets = unpackOffsets(packed);
        if (!Arrays.equals(offsets, vertexOffsets))
        {
            vertexOffsets = offsets;
            getModelData().setData(OFFSETS, packed);

            needUpdate = true;
        }

        int faceIdx = nbt.getInt("face");
        Direction face = faceIdx == -1 ? null : Direction.from3DDataValue(faceIdx);
        if (collapsedFace != face)
        {
            collapsedFace = face;
            getModelData().setData(COLLAPSED_FACE, collapsedFace);

            needUpdate = true;
        }

        return needUpdate;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbt = super.getUpdateTag();
        nbt.putInt("offsets", packOffsets(vertexOffsets));
        nbt.putInt("face", collapsedFace == null ? -1 : collapsedFace.get3DDataValue());
        return nbt;
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt)
    {
        super.handleUpdateTag(nbt);

        int packed = nbt.getInt("offsets");
        vertexOffsets = unpackOffsets(packed);
        getModelData().setData(OFFSETS, packed);

        int face = nbt.getInt("face");
        collapsedFace = face == -1 ? null : Direction.from3DDataValue(face);
        getModelData().setData(COLLAPSED_FACE, collapsedFace);
    }

    @Override
    public CompoundTag save(CompoundTag nbt)
    {
        nbt.putInt("offsets", packOffsets(vertexOffsets));
        nbt.putInt("face", collapsedFace == null ? -1 : collapsedFace.get3DDataValue());
        return super.save(nbt);
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);
        vertexOffsets = unpackOffsets(nbt.getInt("offsets"));
        int face = nbt.getInt("face");
        collapsedFace = face == -1 ? null : Direction.from3DDataValue(face);
    }



    public static int packOffsets(byte[] offsets)
    {
        int result = 0;

        for (int i = 0; i < 4; i++)
        {
            result |= (offsets[i] << (i * 5));
        }

        return result;
    }

    public static byte[] unpackOffsets(int packed)
    {
        byte[] offsets = new byte[4];

        for (int i = 0; i < 4; i++)
        {
            offsets[i] = (byte) (packed >> (i * 5) & 0x1F);
        }

        return offsets;
    }
}