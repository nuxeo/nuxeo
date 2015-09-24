/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Seam component that exposes getters for properties managed by the {@link ConfigurationService} or the runtime
 * {@link Framework}.
 *
 * @since 5.5
 */
@Name("frameworkPropertyActions")
@Scope(ScopeType.STATELESS)
@Install(precedence = FRAMEWORK)
public class FrameworkPropertyActions {

    @In(create = true)
    protected transient ConfigurationService configurationService;

    /**
     * Returns the given property value from the {@link ConfigurationService} if any, otherwise null.
     */
    public String getProperty(String propertyName) {
        return configurationService.getProperty(propertyName);
    }

    /**
     * Returns the given property value from the {@link ConfigurationService} if any, otherwise the given default value.
     */
    public String getProperty(String propertyName, String defaultValue) {
        return configurationService.getProperty(propertyName, defaultValue);
    }

    /**
     * Returns true if given property has been setup to true (defaults to false if not set).
     *
     * @since 5.8
     * @see {@link ConfigurationService#isBooleanPropertyTrue(String)}
     */
    public boolean isBooleanPropertyTrue(String propertyName) {
        return configurationService.isBooleanPropertyTrue(propertyName);
    }

    /**
     * Returns the given property value from the {@link Framework} if any, otherwise null.
     *
     * @since 7.4
     */
    public String getFrameworkProperty(String key) {
        return Framework.getProperty(key);
    }

    /**
     * Returns the given property value from the {@link Framework} if any, otherwise the given default value.
     *
     * @since 7.4
     */
    public String getFrameworkProperty(String key, String defValue) {
        return Framework.getProperty(key, defValue);
    }

}
