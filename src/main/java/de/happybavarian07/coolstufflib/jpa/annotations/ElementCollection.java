package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ElementCollection {
    String tableName() default "";
    String columnName() default "";
    FetchType fetch() default FetchType.EAGER;
}

