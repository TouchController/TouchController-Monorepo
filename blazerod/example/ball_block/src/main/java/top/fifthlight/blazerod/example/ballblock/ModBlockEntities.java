package top.fifthlight.blazerod.example.ballblock;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

// Shamelessly copied from https://docs.fabricmc.net/zh_cn/develop/blocks/block-entities
public class ModBlockEntities {
    public static final BlockEntityType<BallBlockEntity> BALL_BLOCK_ENTITY = register("ball", BallBlockEntity::new, ModBlocks.BALL);

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        var id = Identifier.of(BallBlockMod.MOD_ID, name);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build());
    }
}
