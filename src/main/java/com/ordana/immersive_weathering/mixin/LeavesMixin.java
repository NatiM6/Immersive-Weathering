package com.ordana.immersive_weathering.mixin;

import com.ordana.immersive_weathering.registry.ModTags;
import com.ordana.immersive_weathering.registry.blocks.LeafPileBlock;
import com.ordana.immersive_weathering.registry.blocks.WeatheringHelper;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LeavesBlock.class)
public abstract class LeavesMixin extends Block implements Fertilizable {

    public LeavesMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {

        //Drastically reduced this chance to help lag
        if (!state.get(LeavesBlock.PERSISTENT) && random.nextFloat() < 0.1f) {

            var leafPile = WeatheringHelper.getFallenLeafPile(state).orElse(null);
            if (leafPile != null && world.getBlockState(pos.down()).isIn(ModTags.LEAF_PILE_REPLACEABLE)) {
                if (!world.isChunkLoaded(pos)) return;
                BlockPos targetPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
                int maxFallenLeavesReach = 16;
                int dist = pos.getY() - targetPos.getY();
                if (dist < maxFallenLeavesReach && dist>0) {

                    BlockState replaceState = world.getBlockState(targetPos);

                    boolean isOnLeaf = replaceState.getBlock() instanceof LeafPileBlock;

                    BlockState baseLeaf = leafPile.getDefaultState().with(LeafPileBlock.LAYERS,0);
                    //if we find a non-air block we check if its upper face is sturdy. Given previous iteration if we are not on the first cycle blocks above must be air
                    if (isOnLeaf ||
                            (replaceState.isIn(ModTags.LEAF_PILE_REPLACEABLE) && baseLeaf.canPlaceAt(world,targetPos)
                                    && !WeatheringHelper.hasEnoughBlocksAround(targetPos, 2, 1, 2,
                                    world, b -> b.getBlock() instanceof LeafPileBlock, 6))) {


                        int pileHeight = 0;
                        if(world.getBlockState(targetPos.down()).isOf(Blocks.WATER)){
                            world.setBlockState(targetPos, baseLeaf.with(LeafPileBlock.LAYERS, 0), 2);
                        }
                        else {
                            for (Direction direction : Direction.Type.HORIZONTAL) {
                                BlockState neighbor = world.getBlockState(targetPos.offset(direction));
                                if (!isOnLeaf && neighbor.getBlock() instanceof LeafPileBlock) {
                                    pileHeight = 1;
                                }
                                else if (WeatheringHelper.isLog(neighbor)) {
                                    pileHeight = isOnLeaf ? 2 : 1;
                                    break;
                                }

                            }
                            if (pileHeight > 0) {
                                world.setBlockState(targetPos, baseLeaf.with(LeafPileBlock.LAYERS, pileHeight), 2);
                            }
                        }
                    }

                }
            }
        }
    }

    @Override
    public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
        return state.isOf(Blocks.FLOWERING_AZALEA_LEAVES);
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return state.isOf(Blocks.FLOWERING_AZALEA_LEAVES);
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        for (var direction : Direction.values()) {
            if (random.nextFloat() > 0.5f) {
                var targetPos = pos.offset(direction);
                BlockState targetBlock = world.getBlockState(targetPos);
                WeatheringHelper.getAzaleaGrowth(targetBlock).ifPresent(s ->
                        world.setBlockState(targetPos, s)
                );
            }
        }
    }
}

