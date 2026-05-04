package com.heypixel.heypixel.VcX6svVqmeT8.events.impl;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class EventDestroyBlock implements Event {
   private final BlockPos pos;
   private final Direction face;

   public EventDestroyBlock(BlockPos pos, Direction face) {
      this.pos = pos;
      this.face = face;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Direction getFace() {
      return this.face;
   }
}
