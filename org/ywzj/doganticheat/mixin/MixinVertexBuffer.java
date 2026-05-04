package org.ywzj.doganticheat.mixin;

import com.heypixel.heypixel.VcX6svVqmeT8.utils.renderer.GL;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder.DrawState;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.doganticheat.mixin.accessors.ShapeIndexBufferAccessor;

@Mixin({VertexBuffer.class})
public abstract class MixinVertexBuffer {
   @Shadow
   private int indexBufferId;

   @Inject(
      method = {"uploadIndexBuffer"},
      at = {@At("RETURN")}
   )
   private void onConfigureIndexBuffer(DrawState arg, ByteBuffer byteBuffer, CallbackInfoReturnable<AutoStorageIndexBuffer> info) {
      if (info.getReturnValue() == null) {
         GL.CURRENT_IBO = this.indexBufferId;
      } else {
         GL.CURRENT_IBO = ((ShapeIndexBufferAccessor)info.getReturnValue()).getId();
      }
   }
}
