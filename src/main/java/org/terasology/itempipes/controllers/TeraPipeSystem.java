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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.lifespan.LifespanComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.PickupComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
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
import org.terasology.itempipes.blocks.PipeBlockFamily;
import org.terasology.itempipes.components.PipeComponent;
import org.terasology.itempipes.components.PipeFollowingComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.family.BlockFamily;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = TeraPipeSystem.class)
public class TeraPipeSystem extends BaseComponentSystem {


    @In
    private Time time;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private SegmentSystem segmentSystem;
    @In
    private SegmentCacheSystem segmentCacheSystem;


    public boolean isConnected(Vector3i location, Side side) {
        Vector3i toTest = location.add(side.getVector3i());
        if (worldProvider.isBlockRelevant(toTest)) {
            Block block = worldProvider.getBlock(toTest);
            final BlockFamily blockFamily = block.getBlockFamily();
            EntityRef pipeEntity = blockEntityRegistry.getBlockEntityAt(toTest);
            if (blockFamily instanceof PipeBlockFamily) {
                PathDescriptorComponent pathDescriptor = pipeEntity.getComponent(PathDescriptorComponent.class);
                EnumSet<Side> sides = ((PipeBlockFamily) blockFamily).getSides(block.getURI());
                for (Side s : sides) {
                    if (s.reverse().equals(side)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public Set<Prefab> findingMatchingPathPrefab(EntityRef pipe, Side side) {
        PathDescriptorComponent pathDescriptor = pipe.getComponent(PathDescriptorComponent.class);
        Quat4f rotation = segmentSystem.segmentRotation(pipe);
        Set<Prefab> results = Sets.newHashSet();
        for (Prefab path : pathDescriptor.descriptors) {
            BlockMappingComponent blockMappingComponent = path.getComponent(BlockMappingComponent.class);
            Side s1 = Side.inDirection(rotation.rotate(blockMappingComponent.s1.getVector3i().toVector3f()));
            Side s2 = Side.inDirection(rotation.rotate(blockMappingComponent.s2.getVector3i().toVector3f()));
            if (s1.equals(side) || s2.equals(side)) {
                results.add(path);
            }
        }
        return results;
    }

    public Set<Prefab> filterPrefabBySide(Quat4f rot, Set<Prefab> prefabs, Side side) {
        Set<Prefab> result = Sets.newHashSet();
        for (Prefab prefab : prefabs) {
            BlockMappingComponent blockMappingComponent = prefab.getComponent(BlockMappingComponent.class);
            Side s1 = Side.inDirection(rot.rotate(blockMappingComponent.s1.getVector3i().toVector3f()));
            Side s2 = Side.inDirection(rot.rotate(blockMappingComponent.s2.getVector3i().toVector3f()));
            if (s1.equals(side) || s2.equals(side)) {
                result.add(prefab);
            }

        }
        return result;
    }


    public Segment getSegment(Prefab prefab) {
        return segmentCacheSystem.getSegment(prefab);
    }


    public Map<Side, EntityRef> findPipes(Vector3i location) {
        Map<Side, EntityRef> pipes = Maps.newHashMap();
        for (Side side : Side.values()) {
            Vector3i neighborLocation = new Vector3i(location);
            neighborLocation.add(side.getVector3i());
            if (worldProvider.isBlockRelevant(neighborLocation)) {
                Block neighborBlock = worldProvider.getBlock(neighborLocation);
                final BlockFamily blockFamily = neighborBlock.getBlockFamily();
                if (blockFamily instanceof PipeBlockFamily) {
                    pipes.put(side, blockEntityRegistry.getBlockEntityAt(neighborLocation));
                }
            }
        }
        return pipes;
    }

    public void dropItem(EntityRef actor) {
        ItemComponent itemComponent = actor.getComponent(ItemComponent.class);

        Prefab prefab = itemComponent.pickupPrefab;
        if(itemComponent == null || itemComponent.pickupPrefab == null)
            prefab = actor.getParentPrefab();

        PickupComponent pickupComponent = prefab.getComponent(PickupComponent.class);
        pickupComponent.timeDropped = time.getGameTimeInMs();
        RigidBodyComponent rigidBodyComponent = prefab.getComponent(RigidBodyComponent.class);
        LifespanComponent lifespanComponent = prefab.getComponent(LifespanComponent.class);

        actor.addComponent(rigidBodyComponent);
        actor.addComponent(lifespanComponent);
        actor.addComponent(pickupComponent);


        actor.removeComponent(PipeFollowingComponent.class);
        actor.removeComponent(PathFollowerComponent.class);
    }


    public void insertIntoPipe(EntityRef actor, EntityRef pipe, Side side,Prefab prefab, float velocity) {
        if (actor.hasComponent(PipeFollowingComponent.class))
            return;
        if (!pipe.hasComponent(PipeComponent.class))
            return;

        BlockMappingComponent blockMappingComponent = prefab.getComponent(BlockMappingComponent.class);
        PathFollowerComponent pathFollowerComponent = new PathFollowerComponent();
        Quat4f rotation = segmentSystem.segmentRotation(pipe);
        if (Side.inDirection(rotation.rotate(blockMappingComponent.s1.getVector3i().toVector3f())).equals(side)) {
            pathFollowerComponent.segmentMeta = new SegmentMeta(0, pipe, prefab);
            pathFollowerComponent.segmentMeta.sign = 1;

        } else if (Side.inDirection(rotation.rotate(blockMappingComponent.s2.getVector3i().toVector3f())).equals(side)) {
            Segment segment = segmentCacheSystem.getSegment(prefab);
            pathFollowerComponent.segmentMeta = new SegmentMeta(segment.maxDistance(), pipe, prefab);
            pathFollowerComponent.segmentMeta.sign = -1;
        } else {
            return;
        }


        PipeFollowingComponent pipeFollowingComponent = new PipeFollowingComponent();
        pipeFollowingComponent.velocity = Math.abs(velocity);

        LocationComponent locationComponent = actor.getComponent(LocationComponent.class);
        locationComponent.setWorldRotation(new Quat4f(Vector3f.up(), 0));

        actor.saveComponent(locationComponent);
        actor.addOrSaveComponent(pathFollowerComponent);
        actor.addComponent(pipeFollowingComponent);

        actor.removeComponent(PickupComponent.class);
        actor.removeComponent(RigidBodyComponent.class);
        actor.removeComponent(LifespanComponent.class);
    }

}
