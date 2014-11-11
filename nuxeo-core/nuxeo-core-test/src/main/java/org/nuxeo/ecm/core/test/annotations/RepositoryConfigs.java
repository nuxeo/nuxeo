/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Useful to make suites using different configurations.
 * Usage:
 * <pre>
 * @RepositoryConfigs({
 *   @RepositoryConfig(type=BackendType.H2),
 *   @RepositoryConfig(type=BackendType.JCR)
 * })
 * @SuiteClasses( { Test1.class, Test2.class } )
 * @RunWith(MultiNuxeoCoreRunner.class)
 * public class MySuite {
 * }
 * </pre>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepositoryConfigs {

    RepositoryConfig[] value() default { @RepositoryConfig };

}
