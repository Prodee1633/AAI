package com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.callables;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.Cancellable;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {
   public boolean cancelled;

   protected EventCancellable() {
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean state) {
      this.cancelled = state;
   }
}
