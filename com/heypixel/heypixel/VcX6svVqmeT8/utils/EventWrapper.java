package com.heypixel.heypixel.VcX6svVqmeT8.utils;

import com.heypixel.heypixel.VcX6svVqmeT8.Naven;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventTarget;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.types.EventType;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventClientChat;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventMotion;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRespawn;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventWrapper {
   @SubscribeEvent
   public void onRender(Post e) {
   }

   @SubscribeEvent
   public void onClientChat(ClientChatEvent e) {
      EventClientChat event = new EventClientChat(e.getMessage());
      Naven.getInstance().getEventManager().call(event);
      if (event.isCancelled()) {
         e.setCanceled(true);
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && Minecraft.getInstance().player.tickCount <= 1) {
         Naven.getInstance().getEventManager().call(new EventRespawn());
      }
   }
}
