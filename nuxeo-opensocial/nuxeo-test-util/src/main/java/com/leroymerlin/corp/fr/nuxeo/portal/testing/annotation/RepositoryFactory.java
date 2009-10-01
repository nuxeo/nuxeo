package com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.RepoFactory;


@Inherited
@Retention ( RetentionPolicy.RUNTIME )
@Target ( {ElementType.TYPE , ElementType.METHOD} )
public @interface RepositoryFactory {
    Class<? extends RepoFactory> value();
}
