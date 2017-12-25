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
package org.terasology.terapipes.controllers;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.registry.In;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.terapipes.blocks.PipeBlockSegmentMapper;
import org.terasology.terapipes.components.PipeFollowingComponent;
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

    private PipeBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new PipeBlockSegmentMapper(blockEntityRegistry, pathFollowerSystem,segmentSystem, segmentCacheSystem);
    }
    @Override
    public void update(float delta) {
        for(EntityRef entityRef: entityManager.getEntitiesWith(PipeFollowingComponent.class)) {
            PipeFollowingComponent pipeFollowingComponent = entityRef.getComponent(PipeFollowingComponent.class);
            PathFollowerComponent segmentVehicleComponent = entityRef.getComponent(PathFollowerComponent.class);

            PathFollowerComponent vehicle = entityRef.getComponent(PathFollowerComponent.class);
            vehicle.segmentMeta.association = blockEntityRegistry.getBlockEntityAt(pipeFollowingComponent.blockPosition);
            entityRef.saveComponent(vehicle);

            LocationComponent locationComponent =  entityRef.getComponent(LocationComponent.class);
            Vector3f position = pathFollowerSystem.vehiclePoint(entityRef);
            if (pathFollowerSystem.move(entityRef, delta * .1f, segmentMapping)) {

            }

            pipeFollowingComponent.blockPosition = vehicle.segmentMeta.association.getComponent(BlockComponent.class).getPosition();
            entityRef.saveComponent(pipeFollowingComponent);



            locationComponent.setWorldPosition(position);
            entityRef.saveComponent(locationComponent);
            entityRef.saveComponent(segmentVehicleComponent);

        }
    }

}
