package de.happybavarian07.coolstufflib.service.annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.util.UUID;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServiceComponent {
    String serviceName();
    String uuid() default "";
    String[] dependsOn() default {};
    long startTimeoutMillis() default 20000;
    long stopTimeoutMillis() default 10000;
}
