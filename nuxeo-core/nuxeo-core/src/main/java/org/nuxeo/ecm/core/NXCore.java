/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core;

import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;

/**
 * CoreSession facade for services provided by NXCore module.
 * <p>
 * This is the main entry point to the core services.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXCore {

    private NXCore() {
    }

    /**
     * Returns the life cycle service.
     *
     * @see LifeCycleServiceImpl
     * @return the life cycle service
     */
    public static LifeCycleService getLifeCycleService() {
        return (LifeCycleService) Framework.getRuntime().getComponent(LifeCycleServiceImpl.NAME);
    }

    public static SecurityService getSecurityService() {
        return (SecurityService) Framework.getRuntime().getComponent(SecurityService.NAME);
    }

}
