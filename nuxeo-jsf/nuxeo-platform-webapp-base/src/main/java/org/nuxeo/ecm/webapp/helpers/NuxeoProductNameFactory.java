/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Calendar;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;

@Name("appNameFactory")
@Scope(ScopeType.STATELESS)
@Install(precedence = FRAMEWORK)
public class NuxeoProductNameFactory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Factory(value = "nuxeoApplicationName", scope = ScopeType.APPLICATION)
    public String getNuxeoProductName() {
        return Framework.getProperty(Environment.PRODUCT_NAME);
    }

    @Factory(value = "nuxeoApplicationVersion", scope = ScopeType.APPLICATION)
    public String getNuxeoProductVersion() {
        return Framework.getProperty(Environment.PRODUCT_VERSION);
    }

    /**
     * Gives current year used in copyright (in case this needs to be extracted from a configuration in the future).
     *
     * @since 5.9.2
     */
    @Factory(value = "copyrightCurrentYear", scope = ScopeType.APPLICATION)
    public String getCurrentYearAsString() {
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }

}
