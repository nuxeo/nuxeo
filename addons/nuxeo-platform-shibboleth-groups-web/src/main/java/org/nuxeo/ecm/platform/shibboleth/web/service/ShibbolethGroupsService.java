/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.web.service;

/**
 * Interface handling Shibboleth Groups Service methods
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public interface ShibbolethGroupsService {
    /**
     * Get the defined string to be used as hierarchy delimiter. Example : As ":" defined as the hierarchy delimiter.
     * group:name:student
     *
     * @return the string to use
     */
    String getParseString();

    /**
     * Get the base path (using the previously demlimiter) where shibb group will be stored
     *
     * @return
     */
    String getShibbGroupBasePath();
}
