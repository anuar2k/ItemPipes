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
package org.terasology.itempipes.event;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.AbstractConsumableEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.components.BlockMappingComponent;

import java.util.List;

public class PipeMappingEvent extends AbstractConsumableEvent {

    private List<Prefab> paths;
    private Prefab selectedPath;
    private SegmentMeta segmentMeta;
    private Rotation rotation;

    public PipeMappingEvent(List<Prefab> paths, SegmentMeta meta, Rotation rotation) {
        this.paths = paths;
        this.selectedPath = paths.get(0);
        this.segmentMeta = meta;
        this.rotation = rotation;
    }

    public List<Prefab> getPaths() {
        return paths;
    }

    public EntityRef getPathFollowingEntity() {
        return segmentMeta.association;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public void setSelectedPath(Prefab path) {
        this.selectedPath = path;
    }

    public Prefab getSelectedPath() {
        return selectedPath;
    }
}
