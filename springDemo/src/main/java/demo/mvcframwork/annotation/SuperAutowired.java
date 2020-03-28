package demo.mvcframwork.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuperAutowired {
    String value() default "";
}
