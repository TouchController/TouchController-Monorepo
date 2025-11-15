package top.fifthlight.blazerod.example.ballblock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

// Shamelessly copied from https://docs.fabricmc.net/zh_cn/develop/blocks/block-entities
public class BallBlockEntity extends BlockEntity {
    public BallBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BALL_BLOCK_ENTITY, pos, state);
    }
}