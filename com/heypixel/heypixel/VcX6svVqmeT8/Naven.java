package com.heypixel.heypixel.VcX6svVqmeT8;

import com.heypixel.heypixel.VcX6svVqmeT8.commands.CommandManager;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventManager;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.EventTarget;
import com.heypixel.heypixel.VcX6svVqmeT8.events.api.types.EventType;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventRunTicks;
import com.heypixel.heypixel.VcX6svVqmeT8.events.impl.EventShutdown;
import com.heypixel.heypixel.VcX6svVqmeT8.files.FileManager;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.ModuleManager;
import com.heypixel.heypixel.VcX6svVqmeT8.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixel.VcX6svVqmeT8.ui.notification.NotificationManager;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.EntityWatcher;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.EventWrapper;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.LogUtils;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.NetworkUtils;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.RotationManager;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.ServerUtils;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.TickTimeHelper;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.renderer.Fonts;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.renderer.PostProcessRenderer;
import com.heypixel.heypixel.VcX6svVqmeT8.utils.renderer.Shaders;
import com.heypixel.heypixel.VcX6svVqmeT8.values.HasValueManager;
import com.heypixel.heypixel.VcX6svVqmeT8.values.ValueManager;
import java.awt.FontFormatException;
import java.io.IOException;
import net.minecraftforge.common.MinecraftForge;

public class Naven {
   public static final String CLIENT_NAME = "Naven-Modern";
   public static final String CLIENT_DISPLAY_NAME = "Naven";
   private static Naven instance;
   private final EventManager eventManager;
   private final EventWrapper eventWrapper;
   private final ValueManager valueManager;
   private final HasValueManager hasValueManager;
   private final ModuleManager moduleManager;
   private final CommandManager commandManager;
   private final FileManager fileManager;
   private final NotificationManager notificationManager;
   public static float TICK_TIMER = 1.0F;

   private Naven() {
      System.out.println("Naven Init");
      instance = this;
      this.eventManager = new EventManager();
      Shaders.init();
      PostProcessRenderer.init();

      try {
         Fonts.loadFonts();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      } catch (FontFormatException var3) {
         throw new RuntimeException(var3);
      }

      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.moduleManager = new ModuleManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();
      this.fileManager.load();
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      MinecraftForge.EVENT_BUS.register(this.eventWrapper);
   }

   public static void modRegister() {
      try {
         new Naven();
      } catch (Exception var1) {
         System.err.println("Failed to load client");
         var1.printStackTrace(System.err);
      }
   }

   @EventTarget
   public void onShutdown(EventShutdown e) {
      this.fileManager.save();
      LogUtils.close();
   }

   @EventTarget(0)
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE) {
         TickTimeHelper.update();
      }
   }

   public static Naven getInstance() {
      return instance;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventWrapper getEventWrapper() {
      return this.eventWrapper;
   }

   public ValueManager getValueManager() {
      return this.valueManager;
   }

   public HasValueManager getHasValueManager() {
      return this.hasValueManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public FileManager getFileManager() {
      return this.fileManager;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
}
