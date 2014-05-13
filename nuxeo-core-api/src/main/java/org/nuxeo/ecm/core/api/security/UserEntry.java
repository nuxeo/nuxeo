/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    void addPrivilege(String permission, boolean granted,
            boolean readOnly);

}
