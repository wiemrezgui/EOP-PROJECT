package com.EOP.common_lib.common.annotation;

import com.EOP.common_lib.common.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireDepartmentPermission {
    String service();
    String action();
    Role[] allowedRoles() default {};
}
