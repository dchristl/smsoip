package de.christl.smsoip.annotations;


import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface APIVersion {
    int minVersion() default 13;
}
