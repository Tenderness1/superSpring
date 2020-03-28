package demo.mvcframwork.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuperRequestParam {
    String value() default "";
}
