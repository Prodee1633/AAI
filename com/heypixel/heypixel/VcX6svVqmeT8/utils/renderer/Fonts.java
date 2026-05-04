package com.heypixel.heypixel.VcX6svVqmeT8.utils.renderer;

import com.heypixel.heypixel.VcX6svVqmeT8.utils.renderer.text.CustomTextRenderer;
import java.awt.FontFormatException;
import java.io.IOException;

public class Fonts {
   public static CustomTextRenderer opensans;
   public static CustomTextRenderer harmony;
   public static CustomTextRenderer icons;

   public static void loadFonts() throws IOException, FontFormatException {
      opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
      harmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
      icons = new CustomTextRenderer("icon", 32, 59648, 59652, 512);
   }
}
