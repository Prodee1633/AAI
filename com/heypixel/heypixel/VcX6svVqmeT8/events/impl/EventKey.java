package com.heypixel.heypixel.VcX6svVqmeT8.events.impl;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.callables.EventCancellable;

public class EventKey extends EventCancellable {
   private final int key;
   private final boolean state;

   public int getKey() {
      return this.key;
   }

   public boolean isState() {
      return this.state;
   }

   public EventKey(int key, boolean state) {
      this.key = key;
      this.state = state;
   }
}
