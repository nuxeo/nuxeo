/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;
import java.util.Set;

/**
 * A user name, and a set of permissions granted/denied.
 */
public interface UserEntry extends Serializable {

    String getUserName();

    Set<String> getGrantedPermissions();

    Set<String> getDeniedPermissions();

    /**
     * Adds a granted permission for this username.
     *
     * @since 5.9.4
     * @param permission the permission
     */
    void addPrivilege(String permission);

    /**
     * Adds a permission for this username.
     *
     * @since 5.9.4
     * @param permission the permission
     * @param granted whether the permission is granted or denied
     */
    void addPrivilege(String permission, boolean granted);

    /**
     * Adds a permission for this username.
     *
     * @deprecated since 5.9.4 readonly is not used
     */
    @Deprecated
    void addPrivilege(String permission, boolean granted, boolean readOnly);

}
