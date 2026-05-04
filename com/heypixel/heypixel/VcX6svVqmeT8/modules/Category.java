package com.heypixel.heypixel.VcX6svVqmeT8.modules;

import com.heypixel.heypixel.VcX6svVqmeT8.utils.FontIcons;

public enum Category {
   COMBAT("Combat", FontIcons.SWORD),
   MOVEMENT("Movement", FontIcons.RUNNING),
   RENDER("Render", FontIcons.EYE),
   MISC("Misc", FontIcons.OTHER);

   private final String displayName;
   private final String icon;

   private Category(String displayName, String icon) {
      this.displayName = displayName;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIcon() {
      return this.icon;
   }
}
