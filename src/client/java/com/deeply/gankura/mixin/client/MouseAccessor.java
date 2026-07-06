package com.deeply.gankura.mixin.client;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface MouseAccessor {
    @Accessor("x")
    void gankura$setX(double x);

    @Accessor("y")
    void gankura$setY(double y);
}
