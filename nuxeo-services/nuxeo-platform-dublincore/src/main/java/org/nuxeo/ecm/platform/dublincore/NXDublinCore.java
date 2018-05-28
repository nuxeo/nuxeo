/* 
 * (C) Copyright 2006-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore;

import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;
import org.nuxeo.runtime.api.Framework;

/**
 * DublinCore service facade.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXDublinCore {

    // This is a utility class.
    private NXDublinCore() {
    }

    /**
     * Locates the core service using NXRuntime.
     */
    public static DublinCoreStorageService getDublinCoreStorageService() {
        return (DublinCoreStorageService) Framework.getRuntime().getComponent(DublinCoreStorageService.ID);
    }

}
