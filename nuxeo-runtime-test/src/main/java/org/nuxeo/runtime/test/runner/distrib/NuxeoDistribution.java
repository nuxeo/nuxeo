/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner.distrib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NuxeoDistribution {

    /**
     * The distribution name.
     * if config is not specified will try to locate a distribution configuration matching the profile.
     */
    String profile();

    /**
     * An URL that points to a custom distribution configuration.
     * Use "java:path_to_resource" to locate the configuration using the classloader
     */
    String config() default "";

    /**
     * The nuxeo server home.
     * Can use variables like {profile}, {tmp} for the temporary directory and ~ for the home directory.
     */
    String home() default "~/.nxserver/distrib/{profile}";

    String host() default "localhost";

    int port() default 8989;

    boolean useCache() default true;

    boolean offline() default false;

    String updatePolicy() default "daily";

}
