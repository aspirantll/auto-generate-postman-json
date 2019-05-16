package com.flushest.postman;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author liulei
 * @date 2019/5/15
 * @desc
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ControllerBeanPostProcessor.class)
public @interface EnableAutoGenerateJson {
}
