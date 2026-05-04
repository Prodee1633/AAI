package com.heypixel.heypixel.VcX6svVqmeT8.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
   String name();

   String description();

   Category category();
}
