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
package org.terasology.terapipes.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.inventory.PickupComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Side;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.terapipes.components.SuctionComponent;
import org.terasology.terapipes.controllers.TeraPipeSystem;
import org.terasology.world.block.BlockComponent;

import javax.print.attribute.standard.Sides;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SuctionAction  extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(SuctionAction.class);

    @In
    EntityManager entityManager;

    @In
    TeraPipeSystem teraPipeSystem;

    @Override
    public void update(float delta) {

        for (EntityRef suctionBlock :entityManager.getEntitiesWith(SuctionComponent.class, BlockComponent.class))
        {
            for (EntityRef item : entityManager.getEntitiesWith(LocationComponent.class,RigidBodyComponent.class, PickupComponent.class)) {
                BlockComponent blockComponent =  suctionBlock.getComponent(BlockComponent.class);

                LocationComponent locationComponent = item.getComponent(LocationComponent.class);
                if(blockComponent.getPosition().toVector3f().distance(locationComponent.getWorldPosition()) <= 1f){
                    EntityRef[] refs = teraPipeSystem.findPipes(blockComponent.getPosition());
                    for(int x = 0; x < refs.length; x++)
                    {
                        BlockComponent b =  refs[x].getComponent(BlockComponent.class);
                        teraPipeSystem.insertIntoPipe(Side.inDirection(b.getPosition().toVector3f().sub(blockComponent.getPosition().toVector3f())),item,refs[x]);
                        return;
                    }

                }

                item.send(new ImpulseEvent(blockComponent.getPosition().toVector3f().sub(locationComponent.getWorldPosition()).normalize().mul(2)));
            }
        }
    }


}
