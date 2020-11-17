/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.usermapper.service;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.extension.UserMapper;

/**
 * This service allows to map Nuxeo Users with users coming from external system like SSO or IDM.
 *
 * @author tiry
 * @since 7.4
 */
public interface UserMapperService {

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal according to the given userObject
     *
     * @param mappingName the name of the contributed mapping to use
     * @param userObject the native userObject
     * @return the matching {@link NuxeoPrincipal}
     */
    NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(String mappingName, Object userObject) throws NuxeoException;

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal according to the given userObject
     *
     * @param mappingName the name of the contributed mapping to use
     * @param userObject the native userObject
     * @param createIfNeeded flag to allow creation (default is true)
     * @param update flag to run update (default is true)
     * @return the matching {@link NuxeoPrincipal}
     */
    NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(String mappingName, Object userObject, boolean createIfNeeded,
            boolean update, Map<String, Serializable> params) throws NuxeoException;

    /**
     * Wrap the {@link NuxeoPrincipal} as the userObject used in the external authentication system *
     *
     * @param mappingName the name of the contributed mapping to use
     * @param principal the {@link NuxeoPrincipal} to wrap
     * @param nativePrincipal the principal Object in the target system (can be null)
     */
    Object wrapNuxeoPrincipal(String mappingName, NuxeoPrincipal principal, Object nativePrincipal,
            Map<String, Serializable> params) throws NuxeoException;

    /**
     * Gives access to the contributed Mapping names
     */
    Set<String> getAvailableMappings();

    /**
     * returns the named mapper is any
     */
    UserMapper getMapper(String mappingName) throws NuxeoException;
}
