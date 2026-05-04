package com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.misc;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventTarget;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.types.EventType;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventMotion;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.Category;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.Module;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.ModuleInfo;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Fireball;

@ModuleInfo(
   name = "AntiFireball",
   description = "Prevents fireballs from damaging you",
   category = Category.MISC
)
public class AntiFireball extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         Stream<Entity> stream = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), true);
         Optional<Fireball> fireball = stream.filter(entityx -> entityx instanceof Fireball && mc.player.distanceTo(entityx) < 6.0F)
            .map(entityx -> (Fireball)entityx)
            .findFirst();
         if (!fireball.isPresent()) {
            return;
         }

         Fireball entity = fireball.get();
         mc.gameMode.attack(mc.player, entity);
         mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }
}
