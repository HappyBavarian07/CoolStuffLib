package de.happybavarian07.coolstufflib.commandmanagement;/*
 * @Author HappyBavarian07
 * @Date 12.11.2021 | 17:55
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CommandData annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandData {
    boolean playerRequired() default false;
    boolean opRequired() default false;
    boolean allowOnlySubCommandArgsThatFitToSubArgs() default false;
    boolean senderTypeSpecificSubArgs() default false;
}
