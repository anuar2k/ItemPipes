/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.itempipes.blocks;

import gnu.trove.map.TByteObjectMap;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.itempipes.components.PipeComponent;
import org.terasology.itempipes.components.PipeConnectionComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.UpdatesWithNeighboursFamily;

import java.util.EnumSet;
import java.util.List;

public class PipeBlockFamily extends UpdatesWithNeighboursFamily  implements PathFamily{

    private byte connectionSides;
    private TByteObjectMap<Block> blocks;
    private TByteObjectMap<Rotation> rotation;

    public PipeBlockFamily(BlockUri blockUri, List<String> categories, Block archetypeBlock, TByteObjectMap<Block> blocks, byte connectionSides, TByteObjectMap<Rotation> rotation) {
        super(null, blockUri, categories, archetypeBlock, blocks, connectionSides);
        this.connectionSides = connectionSides;
        this.blocks = blocks;
        this.rotation = rotation;
    }

    @Override
    public Block getBlockForPlacement(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Side attachmentSide, Side direction) {
        byte connections = 0;
        for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
            if (connectionCondition(location, connectSide,worldProvider,blockEntityRegistry)) {
                connections += SideBitFlag.getSide(connectSide);
            }
        }
        return blocks.get(connections);
    }

    @Override
    public Block getBlockForNeighborUpdate(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Block oldBlock) {
        byte connections = 0;
        for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
            if (connectionCondition(location, connectSide,worldProvider,blockEntityRegistry)) {
                connections += SideBitFlag.getSide(connectSide);
            }
        }
        return blocks.get(connections);
    }


    public boolean connectionCondition(Vector3i blockLocation, Side connectSide,WorldProvider worldProvider,BlockEntityRegistry blockEntityRegistry) {
        Vector3i neighborLocation = new Vector3i(blockLocation);
        neighborLocation.add(connectSide.getVector3i());

        EntityRef neighborEntity = blockEntityRegistry.getBlockEntityAt(neighborLocation);
        return neighborEntity != null && (neighborEntity.hasComponent(PipeComponent.class) || neighborEntity.hasComponent(PipeConnectionComponent.class));
    }

    public EnumSet<Side> getSides(BlockUri blockUri)
    {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return SideBitFlag.getSides(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }


    public Rotation getRotationFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return rotation.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
