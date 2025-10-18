package com.xiaxin.exprespawn.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DeathLocation {
    private final ResourceKey<Level> dimension;
    private final BlockPos position;
    private final float yRot;
    private final float xRot;

    public DeathLocation(ResourceKey<Level> dimension, BlockPos position, float yRot, float xRot) {
        this.dimension = dimension;
        this.position = position;
        this.yRot = yRot;
        this.xRot = xRot;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPosition() {
        return position;
    }

    public float getYRot() {
        return yRot;
    }

    public float getXRot() {
        return xRot;
    }

    @Override
    public String toString() {
        return String.format("DeathLocation{dimension=%s, position=%s, yRot=%.2f, xRot=%.2f}",
                dimension.location(), position, yRot, xRot);
    }
}