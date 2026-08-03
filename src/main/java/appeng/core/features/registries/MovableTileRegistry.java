/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.core.features.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.exceptions.AppEngException;
import appeng.api.movable.IMovableHandler;
import appeng.api.movable.IMovableRegistry;
import appeng.api.movable.IMovableTile;

public class MovableTileRegistry implements IMovableRegistry {

    private final HashSet<Block> blacklisted = new HashSet<>();

    private final HashMap<Class<? extends TileEntity>, IMovableHandler> Valid = new HashMap<>();
    private final List<Class<? extends TileEntity>> test = new ArrayList<>();
    private final List<IMovableHandler> handlers = new ArrayList<>();

    // Default handler that does nothing (equivalent to a no-op spatial handler)
    private final IMovableHandler defaultHandler = new IMovableHandler() {
        @Override
        public boolean canHandle(Class<? extends TileEntity> myClass, TileEntity te) {
            return true;
        }

        @Override
        public void moveTile(TileEntity te, World world, BlockPos pos) {
            // No-op
        }
    };

    private final IMovableHandler nullHandler = new IMovableHandler() {
        @Override
        public boolean canHandle(Class<? extends TileEntity> myClass, TileEntity te) {
            return false;
        }

        @Override
        public void moveTile(TileEntity te, World world, BlockPos pos) {
            // No-op
        }
    };

    @Override
    public void blacklistBlock(final Block blk) {
        this.blacklisted.add(blk);
    }

    @Override
    public void whiteListTileEntity(final Class<? extends TileEntity> c) {
        if (c.getName().equals(TileEntity.class.getName())) {
            throw new IllegalArgumentException(new AppEngException("Someone tried to make all tiles movable with " + c
                    + ", this is a clear violation of the purpose of the white list."));
        }

        this.test.add(c);
    }

    @Override
    public boolean askToMove(final TileEntity te) {
        final Class myClass = te.getClass();
        IMovableHandler canMove = this.Valid.get(myClass);

        if (canMove == null) {
            canMove = this.testClass(myClass, te);
        }

        if (canMove != this.nullHandler) {
            if (te instanceof IMovableTile) {
                ((IMovableTile) te).prepareToMove();
            }

            te.invalidate();
            return true;
        }

        return false;
    }

    private IMovableHandler testClass(final Class myClass, final TileEntity te) {
        IMovableHandler handler = null;

        // ask handlers...
        for (final IMovableHandler han : this.handlers) {
            if (han.canHandle(myClass, te)) {
                handler = han;
                break;
            }
        }

        // if you have a handler your opted in
        if (handler != null) {
            this.Valid.put(myClass, handler);
            return handler;
        }

        // if your movable our opted in
        if (te instanceof IMovableTile) {
            this.Valid.put(myClass, this.defaultHandler);
            return this.defaultHandler;
        }

        // if you are on the white list your opted in.
        for (final Class<? extends TileEntity> testClass : this.test) {
            if (testClass.isAssignableFrom(myClass)) {
                this.Valid.put(myClass, this.defaultHandler);
                return this.defaultHandler;
            }
        }

        this.Valid.put(myClass, this.nullHandler);
        return this.nullHandler;
    }

    @Override
    public void doneMoving(final TileEntity te) {
        if (te instanceof IMovableTile) {
            final IMovableTile mt = (IMovableTile) te;
            mt.doneMoving();
        }
    }

    @Override
    public void addHandler(final IMovableHandler han) {
        this.handlers.add(han);
    }

    @Override
    public IMovableHandler getHandler(final TileEntity te) {
        final Class myClass = te.getClass();
        final IMovableHandler h = this.Valid.get(myClass);
        return h == null ? this.defaultHandler : h;
    }

    @Override
    public IMovableHandler getDefaultHandler() {
        return this.defaultHandler;
    }

    @Override
    public boolean isBlacklisted(final Block blk) {
        return this.blacklisted.contains(blk);
    }
}
