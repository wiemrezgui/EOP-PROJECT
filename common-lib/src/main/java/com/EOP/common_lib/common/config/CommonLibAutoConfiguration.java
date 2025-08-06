package com.EOP.common_lib.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@ComponentScan(basePackages = "com.EOP.common_lib.common")
@EnableAspectJAutoProxy
public class CommonLibAutoConfiguration {
}
