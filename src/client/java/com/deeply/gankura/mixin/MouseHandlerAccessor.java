package com.deeply.gankura.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void gankura$setXpos(double xpos);

    @Accessor("ypos")
    void gankura$setYpos(double ypos);
}
