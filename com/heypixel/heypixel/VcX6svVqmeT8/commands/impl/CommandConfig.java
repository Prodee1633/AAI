package com.heypixel.heypixel.VcX6svVqmeT8.commands.impl;

import com.heypixel.heypixel.VcX6svVqmeT8.commands.Command;
import com.heypixel.heypixel.VcX6svVqmeT8.commands.CommandInfo;
import com.heypixel.heypixel.VcX6svVqmeT8.files.FileManager;
import java.io.IOException;

@CommandInfo(
   name = "config",
   description = "Open client config folder.",
   aliases = {"conf"}
)
public class CommandConfig extends Command {
   @Override
   public void onCommand(String[] args) {
      try {
         Runtime.getRuntime().exec("explorer " + FileManager.clientFolder.getAbsolutePath());
      } catch (IOException var3) {
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}
