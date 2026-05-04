package com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.callables;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.Event;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.Typed;

public abstract class EventTyped implements Event, Typed {
   private final byte type;

   protected EventTyped(byte eventType) {
      this.type = eventType;
   }

   @Override
   public byte getType() {
      return this.type;
   }
}
