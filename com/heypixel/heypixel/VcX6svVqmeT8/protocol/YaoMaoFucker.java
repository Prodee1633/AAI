package com.heypixel.heypixel.VcX6svVqmeT8.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Safe build stub.
 *
 * The decompiled original implementation attempted to collect and spoof local
 * hardware identifiers through reflection and Windows-specific commands. That
 * code was also the source of checked-exception compile failures in Java 17.
 * Keep the public API intact for callers, but do not perform hardware probing
 * or hardware-ID spoofing.
 */
public class YaoMaoFucker {
   private static String name = "Unknown CPU";
   private static String cpuId = "";
   private static String manufacturer = "Unknown";
   private static String version = "Unknown";
   private static String bbid = "";
   private static String model = "Unknown";
   private static Object fakeDisk;

   public static String[] getBaseboardInfo() {
      return new String[]{manufacturer, bbid, version, model};
   }

   public static void init(Random random1) {
      name = "Unknown CPU";
      cpuId = "";
      manufacturer = "Unknown";
      version = "Unknown";
      bbid = "";
      model = "Unknown";
      fakeDisk = null;
   }

   public static Object getFakeDisk() {
      return fakeDisk;
   }

   private static Object makeDisk() {
      return null;
   }

   public static List<Object> getFakeInterfaces() {
      return new ArrayList<>();
   }

   public static String getRealHD() {
      return "";
   }

   private static String getFakeName() {
      return "Unknown Network Interface";
   }

   public static Object makeFake(String displayName, String macAddr) {
      return null;
   }

   public static List<String> generateRealAddr() {
      return new ArrayList<>();
   }

   public static String getRealBaseboard() {
      return "";
   }

   public static String getFakeCpu() {
      return name;
   }

   public static String getFakeCpuIdf() {
      return cpuId;
   }

   public static String formatCore(String abbreviation) {
      return abbreviation == null ? "" : abbreviation;
   }

   public static String findGeneration(String cpuModel) {
      return "";
   }

   public static String getGeneration(String abbreviation) {
      return "";
   }

   public static String getModel(String abbreviation) {
      return "";
   }

   public static String getRealId() {
      return "";
   }

   public static String[] cpu() {
      return new String[]{name, cpuId};
   }
}
