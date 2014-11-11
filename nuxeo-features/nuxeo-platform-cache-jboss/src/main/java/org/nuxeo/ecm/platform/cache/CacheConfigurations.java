/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache;

/**
 * Different cache configurations reside in XML files referred by constants in
 * this class.
 *
 * @author DM
 */
public class CacheConfigurations {

    public static final String SERVICE_PROP_INVALIDATION_SYNC = "META-INF/invalidationSync-service.xml";

    public static final String SERVICE_PROP_REPLICATE_SYNC = "META-INF/replSync-service.xml";

    public static final String SERVICE_PROP_DEFAULT = SERVICE_PROP_REPLICATE_SYNC;

    // Constant utility class.
    private CacheConfigurations() {
    }

}
