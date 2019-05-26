/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component that exposes getters for properties managed by the runtime {@link Framework}.
 *
 * @since 5.5
 */
@Name("frameworkPropertyActions")
@Scope(ScopeType.STATELESS)
@Install(precedence = FRAMEWORK)
public class FrameworkPropertyActions {

    /**
     * Returns the given property value from the {@link Framework} if any, otherwise null.
     */
    public String getProperty(String propertyName) {
        return Framework.getProperty(propertyName);
    }

    /**
     * Returns the given property value from the {@link Framework} if any, otherwise the given default value.
     */
    public String getProperty(String propertyName, String defaultValue) {
        return Framework.getProperty(propertyName, defaultValue);
    }

    /**
     * Returns true if given property has been setup to true (defaults to false if not set).
     *
     * @since 5.8
     * @see {@link Framework#isBooleanPropertyTrue(String)}
     */
    public boolean isBooleanPropertyTrue(String propertyName) {
        return Framework.isBooleanPropertyTrue(propertyName);
    }
}
