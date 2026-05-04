package org.ywzj.doganticheat.mixin;

import com.heypixel.heypixel.VcX6svVqmeT8.utils.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientLevel.class})
public class MixinClientLevel {
   @Redirect(
      method = {"tickNonPassenger"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;tick()V"
      )
   )
   public void hookSkipTicks(Entity instance) {
      if (((IMixinMinecraft)Minecraft.getInstance()).getSkipTicks() > 0 && instance == Minecraft.getInstance().player) {
         ((IMixinMinecraft)Minecraft.getInstance()).processSkippedTick();
      } else {
         instance.tick();
      }
   }
}
