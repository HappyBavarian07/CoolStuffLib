package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinColumn {
    String name();

    String referencedColumnName() default "";

    boolean nullable() default true;

    boolean unique() default false;

    String columnDefinition() default "";
}
