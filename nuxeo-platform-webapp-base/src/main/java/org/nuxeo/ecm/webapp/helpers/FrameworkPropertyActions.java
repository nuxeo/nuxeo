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
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component that exposes getters for all properties managements by the
 * runtime {@link Framework}
 * 
 * @since 5.5
 */
@Name("frameworkPropertyActions")
@Scope(ScopeType.STATELESS)
@Install(precedence = FRAMEWORK)
public class FrameworkPropertyActions {

    public String getProperty(String propertyName) {
        return Framework.getProperty(propertyName);
    }

    public String getProperty(String propertyName, String defaultValue) {
        return Framework.getProperty(propertyName, defaultValue);
    }

}
