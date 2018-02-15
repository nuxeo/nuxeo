/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A list of artifacts to be deployed.
 * <p>
 * Deployable artifacts are either bundles either components:
 * <ul>
 * <li>A bundle entry is represented by the bundle symbolic name.
 * <li>A component entry is represented by an URI of the form: symbolicName:componentXmlPath, where symbolicName is the
 * symbolic name of the bundle owning the component.
 * </ul>
 * Example:
 *
 * <pre>
 * &#64;Deploy("org.nuxeo.runtime")
 * &#64;Deploy("org.nuxeo.core:OSGI-INF/component.xml")
 * </pre>
 *
 * @Deprecated since 10.1 use @Deploy instead
 */
@Inherited
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface LocalDeploy {

    /**
     * The local resource URI. The resource uri is of the form bundleId:path/to/resource where resource is a test
     * resource located in src/test/resources that should be resolved using current class loader and deployed as part of
     * the given bundle.
     */
    String[] value();

}
