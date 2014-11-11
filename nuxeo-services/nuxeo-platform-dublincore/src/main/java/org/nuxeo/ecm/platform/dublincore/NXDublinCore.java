/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
