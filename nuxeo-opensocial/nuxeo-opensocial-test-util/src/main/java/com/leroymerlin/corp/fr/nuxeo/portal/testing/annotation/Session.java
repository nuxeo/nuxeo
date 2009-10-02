package com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Inherited
@Retention ( RetentionPolicy.RUNTIME )
@Target ( {ElementType.TYPE } )
public @interface Session {
    String user() default "Administrator";
}
