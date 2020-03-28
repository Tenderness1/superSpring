package demo.mvcframwork.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuperSerivce {
    String value() default "";
}
