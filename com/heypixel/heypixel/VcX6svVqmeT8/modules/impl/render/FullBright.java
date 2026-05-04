package com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.render;

import com.heypixel.heypixel.VcX6svVqmeT8.modules.Category;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.Module;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.ModuleInfo;
import com.heypixel.heypixel.VcX6svVqmeT8.values.ValueBuilder;
import com.heypixel.heypixel.VcX6svVqmeT8.values.impl.FloatValue;

@ModuleInfo(
   name = "FullBright",
   description = "Make your world brighter.",
   category = Category.RENDER
)
public class FullBright extends Module {
   public FloatValue brightness = ValueBuilder.create(this, "Brightness")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
}
