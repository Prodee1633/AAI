package com.heypixel.heypixel.VcX6svVqmeT8.utils;

import com.heypixel.heypixel.VcX6svVqmeT8.Naven;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventHandlePacket;
import com.heypixel.heypixel.VcX6svVqmeT8.protocol.HeypixelProtocol;
import io.netty.handler.codec.DecoderException;
import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.thread.BlockableEventLoop;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.slf4j.Logger;

public class MixinProtectionUtils {
   public static <T extends PacketListener> void onEnsureRunningOnSameThread(Logger LOGGER, Packet<T> packet, T listener, BlockableEventLoop<?> executor) throws RunningOnDifferentThreadException {
      if (!executor.isSameThread()) {
         executor.executeIfPossible(() -> {
            if (listener.isAcceptingMessages()) {
               try {
                  EventHandlePacket event = new EventHandlePacket(packet);
                  Naven.getInstance().getEventManager().call(event);
                  if (event.isCancelled()) {
                     return;
                  }

                  packet.handle(listener);
               } catch (Exception var4) {
                  if (listener.shouldPropagateHandlingExceptions()) {
                     throw var4;
                  }

                  LOGGER.error("Failed to handle packet {}, suppressing error", packet, var4);
               }
            } else {
               LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
            }
         });
         throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
      }
   }

   public static byte[] readByteArray(FriendlyByteBuf buf, int maxSize) {
      int i = buf.readVarInt() - 1;
      if (i > maxSize) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
      } else {
         byte[] abyte = new byte[i];
         buf.readBytes(abyte);
         return abyte;
      }
   }

   public static boolean mixinConnection_injectPacketSendEvent(Logger LOGGER, Packet<?> p_129521_) {
      if (p_129521_ instanceof ServerboundCustomPayloadPacket sb && sb.getIdentifier().toString().equals("heypixel:s2cevent")) {
         FriendlyByteBuf data = sb.getData();
         data.markReaderIndex();
         int id = data.readVarInt();
         boolean temp = mixinConnection_onHeypixelCheckPacket(data, id, LOGGER, sb);
         if (id == 3) {
         }

         data.resetReaderIndex();
         return temp;
      } else {
         return false;
      }
   }

   public static boolean mixinConnection_onHeypixelCheckPacket(FriendlyByteBuf data, int id, Logger LOGGER, ServerboundCustomPayloadPacket sb) {
      ChatUtils.addChatMessage("Send Packet " + id);
      byte[] b = readByteArray(data, data.readableBytes());
      LOGGER.info("ID: {}, {}", id, Arrays.toString(b));
      if (id == 1) {
         LOGGER.info("RE 2");

         try {
            MessageUnpacker messageUnpacker = MessagePack.newDefaultUnpacker(b);
            LOGGER.info("1?");
            long time = messageUnpacker.unpackLong();
            String playerUUID = messageUnpacker.unpackString();
            String randomUUID = messageUnpacker.unpackString();
            int packetID = messageUnpacker.unpackInt();
            if (packetID == 1) {
               int maxClassSize = messageUnpacker.unpackInt();
               Value idkClassSize = messageUnpacker.unpackValue();
               Value classes = messageUnpacker.unpackValue();
               HeypixelProtocol.sendScanClass1(sb.getIdentifier(), time, playerUUID, randomUUID, maxClassSize);
               return true;
            }

            if (packetID == 2) {
            }
         } catch (Exception var14) {
            LOGGER.error(var14.getMessage());
            var14.printStackTrace();
         }

         return false;
      } else {
         return false;
      }
   }
}
