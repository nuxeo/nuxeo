/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
