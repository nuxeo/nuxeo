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
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

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
    public static final ACE BLOCK = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);

    private final String username;

    private final String permission;

    private final boolean isGranted;

    private Calendar begin;

    private Calendar end;

    private String creator;

    /**
     * Create an ACE from an id.
     *
     * @since 7.4
     */
    public static ACE fromId(String aceId) {
        if (aceId == null) {
            return null;
        }

        String[] parts = aceId.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException(String.format("Invalid ACE id: %s", aceId));
        }

        String username = parts[0];
        String permission = parts[1];
        boolean isGranted = Boolean.valueOf(parts[2]);

        String creator = null;
        if (parts.length >= 4 && StringUtils.isNotBlank(parts[3])) {
            creator = parts[3];
        }

        Calendar begin = null;
        if (parts.length >= 5 && StringUtils.isNotBlank(parts[4])) {
            begin = Calendar.getInstance();
            begin.setTimeInMillis(Long.valueOf(parts[4]));
        }

        Calendar end = null;
        if (parts.length >= 6 && StringUtils.isNotBlank(parts[5])) {
            end = Calendar.getInstance();
            end.setTimeInMillis(Long.valueOf(parts[5]));
        }

        return new ACE(username, permission, isGranted, creator, begin, end);
    }

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
     * Constructs an ACE for a given username, permission and creator user.
     *
     * @since 7.3
     */
    public ACE(String username, String permission, String creator) {
        this(username, permission, true);
        this.creator = creator;
    }

    /**
     * Constructs an ACE for a given username, permission, creator user, begin and end date.
     *
     * @since 7.3
     */
    public ACE(String username, String permission, String creator, Calendar begin, Calendar end) {
        this(username, permission, true);
        this.creator = creator;
        this.begin = begin;
        this.end = end;
    }

    /**
     * Constructs an ACE for a given username, permission, specifying wether to grand or deny it, creator user, begin
     * and end date.
     *
     * @since 7.3
     */
    public ACE(String username, String permission, boolean isGranted, String creator, Calendar begin, Calendar end) {
        this(username, permission, isGranted);
        this.creator = creator;
        this.begin = begin;
        this.end = end;
    }

    /**
     * Constructs an ACE for a given username and permission, and specifies whether to grant or deny it.
     */
    public ACE(String username, String permission, boolean isGranted) {
        this.username = username;
        this.permission = permission;
        this.isGranted = isGranted;
    }

    public ACE() {
        this(null, null, false);
    }

    /**
     * Returns this ACE id.
     * <p>
     * This id is unique inside a given ACL.
     *
     * @since 7.4
     */
    public String getId() {
        StringBuilder sb = new StringBuilder();
        sb.append(username);
        sb.append(':');
        sb.append(permission);
        sb.append(':');
        sb.append(isGranted);

        if (creator != null) {
            sb.append(':');
            sb.append(creator);
        }

        if (begin != null) {
            sb.append(':');
            sb.append(begin.getTimeInMillis());
        }

        if (end != null) {
            sb.append(':');
            sb.append(end.getTimeInMillis());
        }

        return sb.toString();
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

    public Calendar getBegin() {
        return begin;
    }

    public void setBegin(Calendar begin) {
        this.begin = begin;
    }

    public Calendar getEnd() {
        return end;
    }

    public void setEnd(Calendar end) {
        this.end = end;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ACE) {
            ACE ace = (ACE) obj;
            boolean beginEqual = ace.begin != null ? ace.begin.equals(begin) : begin == null;
            boolean endEqual = ace.end != null ? ace.end.equals(end) : end == null;
            boolean creatorEqual = ace.creator != null ? ace.creator.equals(creator) : creator == null;
            return ace.isGranted == isGranted && ace.username.equals(username) && ace.permission.equals(permission)
                    && creatorEqual && beginEqual && endEqual;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + (isGranted ? 1 : 0);
        hash = hash * 37 + username.hashCode();
        hash = creator != null ? hash * 37 + creator.hashCode() : hash;
        hash = begin != null ? hash * 37 + begin.hashCode() : hash;
        hash = end != null ? hash * 37 + end.hashCode() : hash;
        return hash * 37 + permission.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append("username=" + username);
        sb.append(", ");
        sb.append("permission=" + permission);
        sb.append(", ");
        sb.append("isGranted=" + isGranted);
        sb.append(", ");
        sb.append("creator=" + creator);
        sb.append(", ");
        sb.append("begin=" + (begin != null ? begin.getTimeInMillis() : null));
        sb.append(", ");
        sb.append("end=" + (end != null ? end.getTimeInMillis() : null));
        sb.append(')');
        return sb.toString();
    }

    @Override
    public Object clone() {
        return new ACE(username, permission, isGranted, creator, begin, end);
    }

}
