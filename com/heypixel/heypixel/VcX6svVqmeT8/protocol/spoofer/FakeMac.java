package com.heypixel.heypixel.VcX6svVqmeT8.protocol.spoofer;

import com.heypixel.heypixel.VcX6svVqmeT8.protocol.HeypixelSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Safe build stub: keeps the API stable without generating spoofed MAC/IP data. */
public class FakeMac {
   public static final Map<String, String> MACS = new LinkedHashMap<>();
   public static final List<String> allIpAddresses = new ArrayList<>();

   public static void refresh(HeypixelSession session) {
      MACS.clear();
      allIpAddresses.clear();
      MACS.put("Unknown Network Interface", "00:00:00:00:00:00");
      allIpAddresses.add("127.0.0.1");
   }

   public static String randomMAC(HeypixelSession session) {
      return "00:00:00:00:00:00";
   }
}
