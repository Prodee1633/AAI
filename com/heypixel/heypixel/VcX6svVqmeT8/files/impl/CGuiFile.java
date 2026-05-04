package com.heypixel.heypixel.VcX6svVqmeT8.files.impl;

import com.heypixel.heypixel.VcX6svVqmeT8.files.ClientFile;
import com.heypixel.heypixel.VcX6svVqmeT8.ui.ClickGUI;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class CGuiFile extends ClientFile {
   public CGuiFile() {
      super("clickgui.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      try {
         ClickGUI.windowX = Integer.parseInt(reader.readLine());
         ClickGUI.windowY = Integer.parseInt(reader.readLine());
         ClickGUI.windowWidth = Integer.parseInt(reader.readLine());
         ClickGUI.windowHeight = Integer.parseInt(reader.readLine());
      } catch (Exception var3) {
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      writer.write((int)ClickGUI.windowX + "\n");
      writer.write((int)ClickGUI.windowY + "\n");
      writer.write((int)ClickGUI.windowWidth + "\n");
      writer.write((int)ClickGUI.windowHeight + "\n");
   }
}
