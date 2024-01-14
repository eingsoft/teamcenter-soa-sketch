package com.eingsoft.emop.tc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * The method annotated with TcOperationThroughCredential, will create EphemeralCredentialContextHolder instance and clean up tc login at finally stage.
 *
 */
public @interface TcOperationThroughCredential {

    boolean printPerformanceInfo() default false;
    
    //when there is already a tc login session, won't set the credential context again
    boolean skipWhenSessionIdIsPresent() default false;
}
