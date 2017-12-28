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
package org.terasology.itempipes.controllers;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.itempipes.components.PipeConnectionComponent;
import org.terasology.itempipes.event.PipeInsertEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.itempipes.blocks.PipeBlockSegmentMapper;
import org.terasology.itempipes.components.PipeComponent;
import org.terasology.itempipes.components.PipeFollowingComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class BlockMotionSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    EntityManager entityManager;
    @In
    PathFollowerSystem pathFollowerSystem;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    SegmentSystem segmentSystem;
    @In
    SegmentCacheSystem segmentCacheSystem;
    @In
    TeraPipeSystem teraPipeSystem;

    private PipeBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new PipeBlockSegmentMapper(blockEntityRegistry, pathFollowerSystem,segmentSystem, segmentCacheSystem);
    }
    @Override
    public void update(float delta) {
        for(EntityRef entityRef: entityManager.getEntitiesWith(PipeFollowingComponent.class)) {
            PathFollowerComponent pathFollowingComponent = entityRef.getComponent(PathFollowerComponent.class);
            EntityRef pipe =  pathFollowingComponent.segmentMeta.association;
            if(!pipe.exists()) {
                teraPipeSystem.dropItem(entityRef);
                return;
            }
            PipeComponent pipeComponent = pipe.getComponent(PipeComponent.class);
            PipeFollowingComponent pipeFollowingComponent = entityRef.getComponent(PipeFollowingComponent.class);
            LocationComponent locationComponent =  entityRef.getComponent(LocationComponent.class);

            pipeFollowingComponent.velocity -= pipeComponent.friction * delta;
            if(pipeFollowingComponent.velocity < .5f)
                pipeFollowingComponent.velocity = .5f;

            if (pathFollowerSystem.move(entityRef, delta * pipeFollowingComponent.velocity, segmentMapping)) {
                Vector3f position = pathFollowerSystem.vehiclePoint(entityRef);
                locationComponent.setWorldPosition(position);
            } else {
                Quat4f rotation = segmentSystem.segmentRotation(pathFollowingComponent.segmentMeta);
                BlockMappingComponent blockMappingComponent = pathFollowingComponent.segmentMeta.prefab.getComponent(BlockMappingComponent.class);
                BlockComponent blockComponent = pipe.getComponent(BlockComponent.class);
                Vector3i blockPosition = new Vector3i(blockComponent.getPosition());
                if(pathFollowingComponent.segmentMeta.sign == 1) {
                    blockPosition.add(Side.inDirection(rotation.rotate(blockMappingComponent.s2.getVector3i().toVector3f())).getVector3i());
                } else {
                    blockPosition.add(Side.inDirection(rotation.rotate(blockMappingComponent.s1.getVector3i().toVector3f())).getVector3i());
                }
                EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(blockPosition);
                teraPipeSystem.dropItem(entityRef);
                if(blockEntity.hasComponent(PipeConnectionComponent.class)) {
                    blockEntity.send(new PipeInsertEvent(entityRef,pathFollowingComponent.segmentMeta));
                }

                return;
            }
            entityRef.saveComponent(locationComponent);
            entityRef.saveComponent(pathFollowingComponent);
            entityRef.saveComponent(pipeFollowingComponent);
        }
    }

}
