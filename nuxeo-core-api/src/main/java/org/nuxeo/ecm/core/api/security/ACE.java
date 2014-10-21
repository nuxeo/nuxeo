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

/**
 * Access control entry, assigning a permission to a user.
 * <p>
 * Optionally, the assignment can be denied instead of being granted.
 */
public final class ACE implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /**
     * An ACE that blocks all permissions for everyone.
     *
     * @since 6.0
     */
    public static final ACE BLOCK = new ACE(SecurityConstants.EVERYONE,
            SecurityConstants.EVERYTHING, false);

    private final String username;

    private final String permission;

    private final boolean isGranted;

    /**
     * Constructs an ACE for a given username and permission.
     * <p>
     * The ACE is granted.
     *
     * @since 6.0
     */
    public ACE(String username, String permission) {
        this(username, permission, true);
    }

    /**
     * Constructs an ACE for a given username and permission, and specifies
     * whether to grant or deny it.
     */
    public ACE(String username, String permission, boolean isGranted) {
        this.username = username;
        this.permission = permission;
        this.isGranted = isGranted;
    }

    public ACE() {
        this(null, null, false);
    }

    public String getUsername() {
        return username;
    }

    public String getPermission() {
        return permission;
    }

    /**
     * Checks if this privilege is granted.
     *
     * @return true if the privilege is granted
     */
    public boolean isGranted() {
        return isGranted;
    }

    /**
     * Checks if this privilege is denied.
     *
     * @return true if privilege is denied
     */
    public boolean isDenied() {
        return !isGranted;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ACE) {
            ACE ace = (ACE) obj;
            return ace.isGranted == isGranted
                && ace.username.equals(username)
                && ace.permission.equals(permission);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + (isGranted ? 1 : 0);
        hash = hash * 37 + username.hashCode();
        return hash * 37 + permission.hashCode();
    }

    @Override
    public String toString() {
        return username + ':' + permission + ':' + isGranted;
    }

    @Override
    public Object clone() {
        return new ACE(username, permission, isGranted);
    }

}
