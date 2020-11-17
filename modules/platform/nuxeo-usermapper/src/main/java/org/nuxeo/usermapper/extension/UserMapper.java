/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.usermapper.extension;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.service.UserMapperService;

/**
 * Interface for class providing a named implementation for the {@link UserMapperService}
 *
 * @author tiry
 * @since 7.4
 */
public interface UserMapper {

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal according to the given userObject
     *
     * @param userObject the object representing the user in the external system
     */
    NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject);

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal according to the given userObject
     *
     * @param userObject the object representing the user in the external system
     * @param createIfNeeded flag to allow creation (default is true)
     * @param update flag to run update (default is true)
     */

    NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject, boolean createIfNeeded, boolean update,
            Map<String, Serializable> params);

    /**
     * Wrap the {@link NuxeoPrincipal} as the userObject used in the external authentication system
     *
     * @param principal the NuxeoPrincipal
     * @param nativePrincipal the native object to represent the principal in the target system
     */
    Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object nativePrincipal, Map<String, Serializable> params);

    /**
     * Init callback to receive the parameters set inside the descriptor
     */
    void init(Map<String, String> params) throws Exception;

    /**
     * Release callback : called when the plugin is about to be unloaded
     */
    void release();
}
