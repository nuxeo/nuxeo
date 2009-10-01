package com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.RepoType;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface Repository {
    RepoType value() default RepoType.H2;
}
