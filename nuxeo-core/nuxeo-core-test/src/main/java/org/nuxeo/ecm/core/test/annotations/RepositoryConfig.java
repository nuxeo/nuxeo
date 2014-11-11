/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.core.test.DefaultDatabaseFactory;
import org.nuxeo.ecm.core.test.NoopRepositoryInit;

/**
 * Defines the session parameters to use.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface RepositoryConfig {

    Class<? extends DatabaseHelperFactory> factory() default DefaultDatabaseFactory.class;

    BackendType type() default BackendType.H2;

    String repositoryName() default "test";

    String databaseName() default "nuxeojunittests";

    Class<? extends RepositoryInit> init() default NoopRepositoryInit.class;

    Granularity cleanup() default Granularity.CLASS;

    String user() default "Administrator";
}
