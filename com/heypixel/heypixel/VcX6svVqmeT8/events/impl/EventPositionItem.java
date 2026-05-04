package com.heypixel.heypixel.VcX6svVqmeT8.events.impl;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.callables.EventCancellable;
import net.minecraft.network.protocol.Packet;

public class EventPositionItem extends EventCancellable {
   private Packet<?> packet;

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventPositionItem(Packet<?> packet) {
      this.packet = packet;
   }
}
