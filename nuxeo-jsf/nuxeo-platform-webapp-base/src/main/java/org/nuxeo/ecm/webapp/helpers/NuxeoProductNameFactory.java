/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.joda.time.DateTime;
import org.nuxeo.runtime.api.Framework;

@Name("appNameFactory")
@Scope(ScopeType.STATELESS)
@Install(precedence = FRAMEWORK)
public class NuxeoProductNameFactory implements Serializable {

    public static String PNAME_KEY = "org.nuxeo.ecm.product.name";

    public static String PVERSION_KEY = "org.nuxeo.ecm.product.version";

    private static final long serialVersionUID = 1L;

    @Factory(value = "nuxeoApplicationName", scope = ScopeType.APPLICATION)
    public String getNuxeoProductName() {
        return Framework.getProperty(PNAME_KEY);
    }

    @Factory(value = "nuxeoApplicationVersion", scope = ScopeType.APPLICATION)
    public String getNuxeoProductVersion() {
        return Framework.getProperty(PVERSION_KEY);
    }

    /**
     * Gives current year used in copyright (in case this needs to be extracted
     * from a configuration in the future).
     *
     * @since 5.9.2
     */
    @Factory(value = "copyrightCurrentYear", scope = ScopeType.APPLICATION)
    public String getCurrentYearAsString() {
        return new DateTime().toString("Y");
    }

}
