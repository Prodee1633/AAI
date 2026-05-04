package com.heypixel.heypixel.VcX6svVqmeT8.utils;

import com.heypixel.heypixel.VcX6svVqmeT8.Naven;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventTarget;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.types.EventType;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventAttackYaw;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventFallFlying;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventJump;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventMotion;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventMoveInput;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventPositionItem;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRayTrace;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRespawn;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRotationAnimation;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRunTicks;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventStrafe;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventUseItemRayTrace;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.combat.AimAssist;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.combat.AttackCrystal;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.combat.Aura;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.move.AutoMLG;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.move.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RotationManager {
   private static final Logger log = LogManager.getLogger(RotationManager.class);
   private static final Minecraft mc = Minecraft.getInstance();
   public static Vector2f rotations;
   public static Vector2f lastRotations;
   public static Vector2f animationRotation;
   public static Vector2f lastAnimationRotation;
   public static boolean active = false;

   public static void setRotations(Vector2f rotations) {
      RotationManager.rotations = rotations;
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      lastRotations = null;
      rotations = null;
   }

   @EventTarget(4)
   public void updateGlobalYaw(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
         Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
         AttackCrystal attackCrystal = (AttackCrystal)Naven.getInstance().getModuleManager().getModule(AttackCrystal.class);
         AutoMLG autoMLG = (AutoMLG)Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
         AimAssist aimAssist = (AimAssist)Naven.getInstance().getModuleManager().getModule(AimAssist.class);
         active = true;
         if (autoMLG.isEnabled() && autoMLG.rotation) {
            setRotations(new Vector2f(mc.player.getYRot(), 90.0F));
         } else if (attackCrystal.isEnabled() && AttackCrystal.rotations != null) {
            setRotations(new Vector2f(AttackCrystal.rotations.x, AttackCrystal.rotations.y));
         } else if (scaffold.isEnabled() && scaffold.rots != null) {
            setRotations(new Vector2f(scaffold.rots.x, scaffold.rots.y));
         } else if (aura.isEnabled() && Aura.target != null && aura.rotation != null) {
            setRotations(new Vector2f(aura.rotation.x, aura.rotation.y));
         } else if (aimAssist.isEnabled() && aimAssist.working) {
            setRotations(new Vector2f(aimAssist.targetRotation.x, aimAssist.targetRotation.y));
         } else {
            active = false;
         }
      }
   }

   @EventTarget
   public void onAnimation(EventRotationAnimation e) {
      if (animationRotation != null && lastAnimationRotation != null) {
         e.setYaw(animationRotation.x);
         e.setLastYaw(lastAnimationRotation.x);
         e.setPitch(animationRotation.y);
         e.setLastPitch(lastAnimationRotation.y);
      }
   }

   @EventTarget(4)
   public void onPre(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (rotations == null || lastRotations == null) {
            rotations = lastRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
         }

         lastAnimationRotation = animationRotation;
         float yaw = rotations.x;
         float pitch = rotations.y;
         if (!Float.isNaN(yaw) && !Float.isNaN(pitch) && active) {
            e.setYaw(yaw);
            e.setPitch(pitch);
         }

         Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
         if (scaffold.isEnabled() && scaffold.mode.isCurrentMode("Normal") && scaffold.snap.getCurrentValue() && scaffold.hideSnap.getCurrentValue()) {
            animationRotation = scaffold.correctRotation;
         } else {
            animationRotation = new Vector2f(e.getYaw(), e.getPitch());
         }

         lastRotations = new Vector2f(e.getYaw(), e.getPitch());
      }
   }

   @EventTarget
   public void onMove(EventMoveInput event) {
      if (active && rotations != null) {
         float yaw = rotations.x;
         MoveUtils.fixMovement(event, yaw);
      }
   }

   @EventTarget
   public void onMove(EventRayTrace event) {
      if (rotations != null && event.entity == mc.player && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onItemRayTrace(EventUseItemRayTrace event) {
      if (rotations != null && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onStrafe(EventStrafe event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget
   public void onJump(EventJump event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget(0)
   public void onPositionItem(EventPositionItem e) {
      if (active && rotations != null) {
         PosRot packet = (PosRot)e.getPacket();
         PosRot newPacket = new PosRot(
            packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), rotations.getX(), rotations.getY(), packet.isOnGround()
         );
         e.setPacket(newPacket);
      }
   }

   @EventTarget
   public void onFallFlying(EventFallFlying e) {
      if (rotations != null) {
         e.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onAttack(EventAttackYaw e) {
      if (rotations != null) {
         e.setYaw(rotations.x);
      }
   }
}
