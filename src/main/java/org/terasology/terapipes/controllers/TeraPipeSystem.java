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

import com.google.common.collect.Lists;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;
import org.terasology.terapipes.blocks.PipeBlockFamily;
import org.terasology.terapipes.components.PipeComponent;
import org.terasology.terapipes.components.PipeFollowingComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.entity.BlockEntitySystem;
import org.terasology.world.block.family.BlockFamily;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = TeraPipeSystem.class)
public class TeraPipeSystem extends BaseComponentSystem {
    @In
    private WorldProvider worldProvider;
    @In
    private BlockEntityRegistry blockEntityRegistry;

    @In
    private SegmentSystem segmentSystem;
    @In
    private SegmentCacheSystem segmentCacheSystem;

    public EntityRef[] findPipes(Vector3i location) {
        List<EntityRef> entities = Lists.newArrayList();

        for (Side side : Side.values()) {
            Vector3i neighborLocation = new Vector3i(location);
            neighborLocation.add(side.getVector3i());
            if (worldProvider.isBlockRelevant(neighborLocation)) {
                Block neighborBlock = worldProvider.getBlock(neighborLocation);
                final BlockFamily blockFamily = neighborBlock.getBlockFamily();
                if (blockFamily instanceof PipeBlockFamily) {
                    entities.add(blockEntityRegistry.getBlockEntityAt(neighborLocation));
                }
            }
        }
        EntityRef[] result = new EntityRef[entities.size()];
        entities.toArray(result);
        return result;
    }


    public void insertIntoPipe(Side side, EntityRef actor, EntityRef pipe) {
        if (!pipe.hasComponent(PipeComponent.class))
            return;

        if (actor.hasComponent(PipeFollowingComponent.class))
            return;

        PathDescriptorComponent pathDescriptor = pipe.getComponent(PathDescriptorComponent.class);
        Vector3i p = pipe.getComponent(BlockComponent.class).getPosition();

        Quat4f rot = segmentSystem.segmentRotation(pipe);
        for (Prefab prefab : pathDescriptor.descriptors) {
            BlockMappingComponent blockMappingComponent = prefab.getComponent(BlockMappingComponent.class);
            Segment segment = segmentCacheSystem.getSegment(prefab);
            if (Side.inDirection(rot.rotate(blockMappingComponent.s1.getVector3i().toVector3f())).reverse().equals(side)) {
                PathFollowerComponent pathFollowerComponent = new PathFollowerComponent();
                pathFollowerComponent.segmentMeta = new SegmentMeta(.5f, pipe, prefab);
                pathFollowerComponent.heading = rot.rotate(blockMappingComponent.s1.getVector3i().toVector3f()).invert();


                RigidBodyComponent rigidBodyComponent = actor.getComponent(RigidBodyComponent.class);
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);
                rigidBodyComponent.kinematic = true;

                PipeFollowingComponent pipeFollowingComponent = new PipeFollowingComponent();
                pipeFollowingComponent.blockPosition = p;

                actor.addOrSaveComponent(pathFollowerComponent);
                actor.addComponent(pipeFollowingComponent);
                actor.saveComponent(rigidBodyComponent);
                return;

            }

            if (Side.inDirection(rot.rotate(blockMappingComponent.s2.getVector3i().toVector3f())).reverse().equals(side)) {
                PathFollowerComponent pathFollowerComponent = new PathFollowerComponent();
                pathFollowerComponent.segmentMeta = new SegmentMeta(segment.maxDistance(), pipe, prefab);
                ;
                pathFollowerComponent.heading = rot.rotate(blockMappingComponent.s2.getVector3i().toVector3f()).invert();

                RigidBodyComponent rigidBodyComponent = actor.getComponent(RigidBodyComponent.class);
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);
                rigidBodyComponent.kinematic = true;

                PipeFollowingComponent pipeFollowingComponent = new PipeFollowingComponent();
                pipeFollowingComponent.blockPosition = p;


                actor.addOrSaveComponent(pathFollowerComponent);
                actor.addComponent(pipeFollowingComponent);
                actor.saveComponent(rigidBodyComponent);
                return;
            }

        }
    }

}
