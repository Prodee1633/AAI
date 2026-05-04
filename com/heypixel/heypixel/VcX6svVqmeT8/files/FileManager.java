package com.heypixel.heypixel.VcX6svVqmeT8.files;

import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventTarget;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRespawn;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.CGuiFile;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.FriendFile;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.KillSaysFile;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.ModuleFile;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.ProxyFile;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.SpammerFile;
import com.heypixel.heypixel.VcX6svVqmeT8.files.impl.ValueFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileManager {
   public static final Logger logger = LogManager.getLogger(FileManager.class);
   public static File clientFolder = resolveClientFolder();
   public static Object trash = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
   private final List<ClientFile> files = new ArrayList<>();

   public FileManager() {
      this.ensureClientFolder();
      this.files.add(new KillSaysFile());
      this.files.add(new SpammerFile());
      this.files.add(new ModuleFile());
      this.files.add(new ValueFile());
      this.files.add(new CGuiFile());
      this.files.add(new ProxyFile());
      this.files.add(new FriendFile());
   }

   public void load() {
      this.updateClientFolder();

      for (ClientFile clientFile : this.files) {
         File file = clientFile.getFile();

         try {
            if (!file.exists() && file.createNewFile()) {
               logger.info("Created file " + file.getName() + "!");
               this.saveFile(clientFile);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
            clientFile.read(reader);
            reader.close();
         } catch (IOException var5) {
            logger.error("Failed to load file " + file.getName() + "!", var5);
            this.saveFile(clientFile);
         }
      }
   }

   public void save() {
      this.updateClientFolder();

      for (ClientFile clientFile : this.files) {
         this.saveFile(clientFile);
      }

      logger.info("Saved all files!");
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      if (this.updateClientFolder()) {
         this.load();
      }
   }

   private void saveFile(ClientFile clientFile) {
      File file = clientFile.getFile();

      try {
         this.ensureClientFolder();

         if (!file.exists() && file.createNewFile()) {
            logger.info("Created file " + file.getName() + "!");
         }

         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
         clientFile.save(writer);
         writer.flush();
         writer.close();
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   private boolean updateClientFolder() {
      File targetFolder = resolveClientFolder();
      boolean changed = !targetFolder.equals(clientFolder);
      clientFolder = targetFolder;
      this.ensureClientFolder();
      return changed;
   }

   private void ensureClientFolder() {
      if (!clientFolder.exists() && clientFolder.mkdirs()) {
         logger.info("Created client folder: " + clientFolder.getAbsolutePath());
      }
   }

   private static File resolveClientFolder() {
      Minecraft minecraft = Minecraft.getInstance();

      if (minecraft != null && minecraft.getSingleplayerServer() != null) {
         Path worldFolder = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
         return worldFolder.resolve("Naven").toFile();
      }

      File gameDirectory = minecraft != null && minecraft.gameDirectory != null ? minecraft.gameDirectory : new File(".");
      return new File(gameDirectory, "Naven");
   }
}
