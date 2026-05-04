package com.heypixel.heypixel.VcX6svVqmeT8.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.ywzj.doganticheat.mixin.accessors.ClientLevelAccessor;

public class PacketUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static void sendSequencedPacket(PredictiveAction packetCreator) {
      if (mc.getConnection() != null && mc.level != null) {
         BlockStatePredictionHandler pendingUpdateManager = ((ClientLevelAccessor)mc.level).getBlockStatePredictionHandler().startPredicting();

         try {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
         } catch (Throwable var5) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }
}
