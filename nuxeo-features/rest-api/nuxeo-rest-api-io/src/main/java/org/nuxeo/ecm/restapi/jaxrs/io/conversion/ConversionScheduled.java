/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.jaxrs.io.conversion;

/**
 * @since 7.4
 */
public class ConversionScheduled {

    public final String id;

    public final String pollingURL;

    public final String resultURL;

    /**
     * @since 8.4
     */
    public ConversionScheduled(String id, String pollingURL, String resultURL) {
        this.id = id;
        this.pollingURL = pollingURL;
        this.resultURL = resultURL;
    }

    public ConversionScheduled(String id, String pollingURL) {
        this(id, pollingURL, null);
    }
}
