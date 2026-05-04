package com.heypixel.heypixel.VcX6svVqmeT8.events.impl;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.Event;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.types.EventType;
import net.minecraft.network.chat.Component;

public class EventRenderTabOverlay implements Event {
   private EventType type;
   private Component component;

   public void setType(EventType type) {
      this.type = type;
   }

   public void setComponent(Component component) {
      this.component = component;
   }

   public EventType getType() {
      return this.type;
   }

   public Component getComponent() {
      return this.component;
   }

   public EventRenderTabOverlay(EventType type, Component component) {
      this.type = type;
      this.component = component;
   }
}
