package com.heypixel.heypixel.VcX6svVqmeT8.protocol.spoofer;

import com.heypixel.heypixel.VcX6svVqmeT8.protocol.HeypixelSession;
import oshi.hardware.common.AbstractBaseboard;

/** Safe build stub: keeps the API stable without generating spoofed baseboard data. */
public class FakeBaseboard extends AbstractBaseboard {
   public HeypixelSession session;

   public String getManufacturer() {
      return "Unknown";
   }

   public String getModel() {
      return "Unknown";
   }

   public String getVersion() {
      return "Unknown";
   }

   public String getSerialNumber() {
      return "";
   }
}
