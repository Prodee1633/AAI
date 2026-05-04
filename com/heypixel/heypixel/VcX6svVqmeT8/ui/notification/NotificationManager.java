package com.heypixel.heypixel.VcX6svVqmeT8.ui.notification;

import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRender2D;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventShader;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;

public class NotificationManager {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }

   public void onRenderShadow(EventShader e) {
      for (Notification notification : this.notifications) {
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         Window window = Minecraft.getInstance().getWindow();
         notification.renderShader(e.getStack(), window.getGuiScaledWidth() - widthTimer.value + 2.0F, window.getGuiScaledHeight() - heightTimer.value);
      }
   }

   public void onRender(EventRender2D e) {
      float height = 5.0F;

      for (Notification notification : this.notifications) {
         e.getStack().pushPose();
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
            if (widthTimer.isAnimationDone(true)) {
               this.notifications.remove(notification);
            }
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }

         widthTimer.update(true);
         heightTimer.update(true);
         Window window = Minecraft.getInstance().getWindow();
         notification.render(e.getStack(), window.getGuiScaledWidth() - widthTimer.value + 2.0F, window.getGuiScaledHeight() - heightTimer.value);
         e.getStack().popPose();
      }
   }
}
