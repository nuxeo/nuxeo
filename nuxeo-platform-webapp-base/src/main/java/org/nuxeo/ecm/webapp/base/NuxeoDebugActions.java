/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.base;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.6
 */
@Name("nxDebugActions")
@Scope(ScopeType.EVENT)
public class NuxeoDebugActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @Factory(value = "nxDebugModeIsEnabled")
    public boolean isDebugModeSet() {
        String debugPropValue = Framework.getProperty(
                ConfigurationGenerator.NUXEO_DEBUG_SYSTEM_PROP, "false");
        return Boolean.TRUE.equals(Boolean.valueOf(debugPropValue));
    }

}
