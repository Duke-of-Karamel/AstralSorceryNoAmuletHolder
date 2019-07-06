/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.tile.base.network;

import hellfirepvp.astralsorcery.common.auxiliary.link.LinkableTileEntity;
import hellfirepvp.astralsorcery.common.starlight.IStarlightReceiver;
import hellfirepvp.astralsorcery.common.starlight.WorldNetworkHandler;
import hellfirepvp.astralsorcery.common.starlight.transmission.IPrismTransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.ITransmissionReceiver;
import hellfirepvp.astralsorcery.common.tile.base.TileNetwork;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileReceiverBase
 * Created by HellFirePvP
 * Date: 30.06.2019 / 20:54
 */
public abstract class TileReceiverBase<T extends ITransmissionReceiver> extends TileNetwork implements IStarlightReceiver<T>, LinkableTileEntity {

    protected TileReceiverBase(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public World getLinkWorld() {
        return getTrWorld();
    }

    @Override
    public BlockPos getLinkPos() {
        return getTrPos();
    }

    @Override
    @Nonnull
    public BlockPos getTrPos() {
        return getPos();
    }

    @Override
    @Nonnull
    public World getTrWorld() {
        return getWorld();
    }

    @Override
    public void onLinkCreate(PlayerEntity player, BlockPos other) {}

    @Override
    public boolean tryLink(PlayerEntity player, BlockPos other) {
        return false;
    }

    @Override
    public boolean tryUnlink(PlayerEntity player, BlockPos other) {
        return false;
    }

    @Override
    public List<BlockPos> getLinkedPositions() {
        return new LinkedList<>();
    }

    @Nullable
    public T tryGetNode() {
        IPrismTransmissionNode node = WorldNetworkHandler.getNetworkHandler(world).getTransmissionNode(getPos());
        if (!(node instanceof ITransmissionReceiver)) {
            return null;
        }
        return (T) node;
    }

}