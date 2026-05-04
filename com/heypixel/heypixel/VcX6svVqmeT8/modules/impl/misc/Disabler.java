package com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.misc;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventTarget;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.types.EventType;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventPacket;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.Category;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.Module;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.ModuleInfo;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.ChatUtils;
import com.heypixel.heypixel.VcX6svVqmeT8.values.ValueBuilder;
import com.heypixel.heypixel.VcX6svVqmeT8.values.impl.BooleanValue;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;

@ModuleInfo(
   name = "Disabler",
   category = Category.MISC,
   description = "Disables some checks of the anti cheat."
)
public class Disabler extends Module {
   private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
   private float playerYaw;
   private float deltaYaw;
   private float lastPlacedDeltaYaw;
   private boolean rotated = false;

   private void log(String message) {
      if (this.logging.getCurrentValue()) {
         ChatUtils.addChatMessage(message);
      }
   }

   @EventTarget(3)
   public void duplicateRotPlaceDisabler(EventPacket e) {
      if (e.getType() == EventType.SEND && !e.isCancelled() && mc.player != null) {
         if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
            ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket)e.getPacket();
            if (packet.hasRotation()) {
               if (packet.getYRot(0.0F) < 360.0F && packet.getYRot(0.0F) > -360.0F) {
                  if (packet.hasPosition()) {
                     e.setPacket(
                        new PosRot(
                           packet.getX(0.0),
                           packet.getY(0.0),
                           packet.getZ(0.0),
                           packet.getYRot(0.0F) + 720.0F,
                           packet.getXRot(0.0F),
                           packet.isOnGround()
                        )
                     );
                  } else {
                     e.setPacket(new Rot(packet.getYRot(0.0F) + 720.0F, packet.getXRot(0.0F), packet.isOnGround()));
                  }
               }

               float lastPlayerYaw = this.playerYaw;
               this.playerYaw = packet.getYRot(0.0F);
               this.deltaYaw = Math.abs(this.playerYaw - lastPlayerYaw);
               this.rotated = true;
               if (this.deltaYaw > 2.0F) {
                  float xDiff = Math.abs(this.deltaYaw - this.lastPlacedDeltaYaw);
                  if (xDiff < 1.0E-4) {
                     this.log("Disabling DuplicateRotPlace!");
                     if (packet.hasPosition()) {
                        e.setPacket(
                           new PosRot(
                              packet.getX(0.0),
                              packet.getY(0.0),
                              packet.getZ(0.0),
                              packet.getYRot(0.0F) + 0.002F,
                              packet.getXRot(0.0F),
                              packet.isOnGround()
                           )
                        );
                     } else {
                        e.setPacket(new Rot(packet.getYRot(0.0F) + 0.002F, packet.getXRot(0.0F), packet.isOnGround()));
                     }
                  }
               }
            }
         } else if (e.getPacket() instanceof ServerboundUseItemOnPacket && this.rotated) {
            this.lastPlacedDeltaYaw = this.deltaYaw;
            this.rotated = false;
         }
      }
   }
}
