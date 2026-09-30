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

    /**
     * Stable ID of the service. Leave it empty (the default) to get an ID derived from
     * {@link #serviceName()}, which is the same ID {@code ServiceDescriptor.of(serviceName)} produces.
     * Set it explicitly only to keep an ID that was handed out before, because a changed ID is
     * a different service for every consumer that stored the old one.
     */
    String uuid() default "";

    String[] dependsOn() default {};
    long startTimeoutMillis() default 20000;
    long stopTimeoutMillis() default 10000;
}
