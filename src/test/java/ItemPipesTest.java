import com.google.api.client.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.itempipes.components.PipeFollowingComponent;
import org.terasology.itempipes.controllers.PipeSystem;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.events.DropItemEvent;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemFactory;
import org.terasology.world.block.items.OnBlockItemPlaced;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ItemPipesTest extends ModuleTestingEnvironment {
    private WorldProvider worldProvider;
    private BlockManager blockManager;
    private BlockEntityRegistry blockEntityRegistry;
    private EntityManager entityManager;
    private PipeSystem pipeSystem;
    private Time time;

    @Override
    public Set<String> getDependencies() {
        Set<String> modules = Sets.newHashSet();
        modules.add("engine");
        modules.add("Core");
        modules.add("SegmentedPaths");
        modules.add("ItemPipes");
        return modules;
    }

    @Before
    public void initialize() {
        worldProvider = getHostContext().get(WorldProvider.class);
        blockManager = getHostContext().get(BlockManager.class);
        blockEntityRegistry = getHostContext().get(BlockEntityRegistry.class);
        entityManager = getHostContext().get(EntityManager.class);
        pipeSystem = getHostContext().get(PipeSystem.class);
        time = getHostContext().get(Time.class);
    }

    @Test
    public void connectionTest() {
        placeBlock(Vector3i.zero(), "ItemPipes:basicPipe");
        assertTrue(getConn(Vector3i.zero()) == 0);

        //begin with 1 to omit the pipe without connections.
        for (byte connections = 1; connections < 64; connections++) {
            EnumSet<Side> sideSet = SideBitFlag.getSides(connections);

            for (Side side : sideSet) {
                placeBlock(side.getVector3i(), "ItemPipes:basicPipe");
            }

            assertTrue(getConn(Vector3i.zero()) == connections);

            for (Side side : sideSet) {
                dealDamageOn(side.getVector3i(), 10000);
            }
        }
    }

    @Test
    public void chestInputTest() {
        placeBlock(Vector3i.west(), "ItemPipes:basicPipe");
        placeBlock(Vector3i.zero(), "ItemPipes:basicPipe");
        placeBlock(Vector3i.east(), "Core:chest");
        EntityRef droppedItem = dropBlockItem(Vector3f.west().add(Vector3f.up()), "ItemPipes:suction");

        EntityRef startPipe = blockEntityRegistry.getBlockEntityAt(Vector3i.zero());
        Prefab pathPrefab = pipeSystem.findingMatchingPathPrefab(startPipe, Side.RIGHT).iterator().next();
        pipeSystem.insertIntoPipe(droppedItem, startPipe, Side.RIGHT, pathPrefab, 1f);

        final long nextCheck = time.getGameTimeInMs()+3000;
        runWhile(() -> getHostContext().get(Time.class).getGameTimeInMs() < nextCheck);

        EntityRef chestEntity = blockEntityRegistry.getBlockEntityAt(Vector3i.east());
        InventoryComponent inventory = chestEntity.getComponent(InventoryComponent.class);

        boolean foundDroppedItem = false;
        for (EntityRef slot : inventory.itemSlots) {
            if (slot == droppedItem) {
                foundDroppedItem = true;
                break;
            }
        }
        assertTrue(foundDroppedItem);
    }

    @Test
    public void minimumVelocityTest() {
        Vector3i newPipeLoc = Vector3i.zero();
        placeBlock(new Vector3i(newPipeLoc), "ItemPipes:basicPipe");
        newPipeLoc.add(Vector3i.up());
        placeBlock(new Vector3i(newPipeLoc), "ItemPipes:basicPipe");
        newPipeLoc.add(Vector3i.east());
        placeBlock(new Vector3i(newPipeLoc), "ItemPipes:basicPipe");
        newPipeLoc.add(Vector3i.down());
        placeBlock(new Vector3i(newPipeLoc), "ItemPipes:basicPipe");

        EntityRef droppedItem = dropBlockItem(Vector3f.west().add(Vector3f.up()), "ItemPipes:suction");
        EntityRef startPipe = blockEntityRegistry.getBlockEntityAt(Vector3i.zero());
        Prefab pathPrefab = pipeSystem.findingMatchingPathPrefab(startPipe, Side.TOP).iterator().next();
        pipeSystem.insertIntoPipe(droppedItem, startPipe, Side.TOP, pathPrefab, 1f);

        for (int i = 0; i < 1000; i++) {
            final long nextCheck = time.getGameTimeInMs()+100;
            runWhile(() -> getHostContext().get(Time.class).getGameTimeInMs() < nextCheck);
            PipeFollowingComponent pfComponent = droppedItem.getComponent(PipeFollowingComponent.class);
            assertTrue(pfComponent.velocity >= .5f || pfComponent.velocity <= -.5f);
        }

    }

    /**
     * Reads connection flags from an block at given location (used with ItemPipes)
     * @param location location of the pipe we want to check.
     * @return byte connection flags.
     */
    private byte getConn(Vector3i location) {
        return Byte.valueOf(worldProvider.getBlock(location).getURI().getIdentifier().toString());
    }

    /**
     * Deals damage to block on given location (simulates the situation when player destroys a block)
     * @param location location of the block to deal damage
     * @param damageAmount amount of the damage to be dealt.
     */
    private EntityRef dealDamageOn(Vector3i location, int damageAmount) {
        EntityRef block = blockEntityRegistry.getBlockEntityAt(location);
        block.send(new DoDamageEvent(damageAmount, EngineDamageTypes.DIRECT.get()));

        return block;
    }

    /**
     * Simulates the situation when a player places a block.
     * @param location location of the block to be placed.
     * @param id ID of the block family.
     */
    private EntityRef placeBlock(Vector3i location, String id) {
        forceAndWaitForGeneration(location);

        BlockItemFactory blockItemFactory = new BlockItemFactory(entityManager);
        BlockFamily family = blockManager.getBlockFamily(id);
        EntityRef newBlock = blockItemFactory.newInstance(family);

        Block block = family.getBlockForPlacement(worldProvider, blockEntityRegistry, location, Side.TOP, Side.FRONT);

        PlaceBlocks placeBlocks = new PlaceBlocks(location, block);
        worldProvider.getWorldEntity().send(placeBlocks);
        newBlock.send(new OnBlockItemPlaced(location, blockEntityRegistry.getBlockEntityAt(location)));

        return newBlock;
    }

    /**
     * Spawns and drops an block item on desired location.
     * @param location location of the item we want to drop.
     * @param id ID of blockItem's family.
     */
    private EntityRef dropBlockItem(Vector3f location, String id) {
        BlockFamily blockFamily = blockManager.getBlockFamily(id);
        BlockItemFactory blockItemFactory = new BlockItemFactory(entityManager);
        EntityRef newBlock = blockItemFactory.newInstance(blockFamily);

        newBlock.send(new DropItemEvent(new Vector3f(location)));

        return newBlock;
    }
}
