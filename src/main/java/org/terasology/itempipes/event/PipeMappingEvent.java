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
import org.terasology.world.block.Block;

import java.util.List;
import java.util.Set;

public class PipeMappingEvent extends AbstractConsumableEvent {
    private Set<Side> outputSides;
    private Side outputSide;

    public PipeMappingEvent(Set<Side> sides) {
        this.outputSides = sides;
        for (Side side : sides) {
            this.outputSide = side;
            break;
        }

    }

    public Set<Side> getOutputSides() {
        return outputSides;
    }

    public Side getOutputSide() {
        return outputSide;
    }

    public void setOutputSide(Side outputSide) {
        this.outputSide = outputSide;
    }
}
