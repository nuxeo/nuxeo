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

package org.nuxeo.ecm.platform.dublincore;

import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;
import org.nuxeo.runtime.api.Framework;

/**
 *  DublinCore service facade.
 *
 *  @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXDublinCore {

    // This is a utility class.
    private NXDublinCore() { }

    /**
     * Locates the core service using NXRuntime.
     */
    public static DublinCoreStorageService getDublinCoreStorageService() {
        return (DublinCoreStorageService) Framework.getRuntime().getComponent(
                DublinCoreStorageService.ID);
    }

}
