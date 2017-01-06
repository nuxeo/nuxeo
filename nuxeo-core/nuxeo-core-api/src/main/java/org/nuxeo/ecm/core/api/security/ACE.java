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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

/**
 * Access control entry, assigning a permission to a user.
 * <p>
 * Optionally, the assignment can be denied instead of being granted.
 */
public final class ACE implements Serializable, Cloneable {

    public enum Status {
        PENDING, EFFECTIVE, ARCHIVED;
    }

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

    private Map<String, Serializable> contextData = new HashMap<>();

    protected static final Pattern p = Pattern.compile("^(.+):([^:]+):([^:]+):([^:]*):([^:]*):([^:]*)$");

    /**
     * Create an ACE from an id.
     *
     * @since 7.4
     */
    public static ACE fromId(String aceId) {
        if (aceId == null) {
            return null;
        }

        // An ACE is composed of tokens separated with ":" caracter
        // First 3 tokens are mandatory; following 3 tokens are optional
        // The ":" separator is still present even if the tokens are empty
        //   Example: jsmith:ReadWrite:true:::
        // The first token (username) is allowed to contain embedded ":".
        String[] parts = new String[6];
        Matcher m = p.matcher(aceId);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Invalid ACE id: %s", aceId));
        }

        for (int i = 1; i <= m.groupCount(); i++) {
            parts[i - 1] = m.group(i);
        }

        String username = parts[0];
        String permission = parts[1];
        boolean isGranted = Boolean.valueOf(parts[2]);

        ACEBuilder builder = ACE.builder(username, permission).isGranted(isGranted);

        if (parts.length >= 4 && StringUtils.isNotBlank(parts[3])) {
            builder.creator(parts[3]);
        }

        if (parts.length >= 5 && StringUtils.isNotBlank(parts[4])) {
            Calendar begin = new GregorianCalendar();
            begin.setTimeInMillis(Long.valueOf(parts[4]));
            builder.begin(begin);
        }

        if (parts.length >= 6 && StringUtils.isNotBlank(parts[5])) {
            Calendar end = new GregorianCalendar();
            end.setTimeInMillis(Long.valueOf(parts[5]));
            builder.end(end);
        }

        return builder.build();
    }

    public ACE() {
        this(null, null, false);
    }

    /**
     * Constructs an ACE for a given username and permission, and specifies whether to grant or deny it.
     */
    public ACE(String username, String permission, boolean isGranted) {
        this(username, permission, isGranted, null, null, null, null);
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
     * Constructs an ACE for a given username, permission, specifying whether to grant or deny it, creator user, begin
     * and end date.
     *
     * @since 7.4
     */
    ACE(String username, String permission, boolean isGranted, String creator, Calendar begin, Calendar end,
            Map<String, Serializable> contextData) {
        this.username = username;
        this.permission = permission;
        this.isGranted = isGranted;
        this.creator = creator;
        setBegin(begin);
        setEnd(end);
        if (contextData != null) {
            this.contextData = new HashMap<>(contextData);
        }

        if (begin != null && end != null) {
            if (begin.after(end)) {
                throw new IllegalArgumentException("'begin' date cannot be after 'end' date");
            }
        }
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

        sb.append(':');
        if (creator != null) {
            sb.append(creator);
        }

        sb.append(':');
        if (begin != null) {
            sb.append(begin.getTimeInMillis());
        }

        sb.append(':');
        if (end != null) {
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

    /**
     * Sets the begin date of this ACE.
     * <p>
     * Sets the {@code Calendar.MILLISECOND} part of the Calendar to 0.
     */
    public void setBegin(Calendar begin) {
        this.begin = begin;
        if (this.begin != null) {
            this.begin.set(Calendar.MILLISECOND, 0);
        }
    }

    public Calendar getEnd() {
        return end;
    }

    /**
     * Sets the end date of this ACE.
     * <p>
     * Sets the {@code Calendar.MILLISECOND} part of the Calendar to 0.
     */
    public void setEnd(Calendar end) {
        this.end = end;
        if (this.end!= null) {
            this.end.set(Calendar.MILLISECOND, 0);
        }
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Returns the status of this ACE.
     *
     * @since 7.4
     */
    public Status getStatus() {
        Status status = Status.EFFECTIVE;
        Calendar now = new GregorianCalendar();
        if (begin != null && now.before(begin)) {
            status = Status.PENDING;
        }
        if (end != null && now.after(end)) {
            status = Status.ARCHIVED;
        }
        return status;
    }

    /**
     * Returns a Long value of this ACE status.
     * <p>
     * It returns {@code null} if there is no begin and end date, which means the ACE is effective. Otherwise, it
     * returns 0 for PENDING, 1 for EFFECTIVE and 2 for ARCHIVED.
     *
     * @since 7.4
     */
    public Long getLongStatus() {
        if (begin == null && end == null) {
            return null;
        }
        return Long.valueOf(getStatus().ordinal());
    }

    public boolean isEffective() {
        return getStatus() == Status.EFFECTIVE;
    }

    public boolean isPending() {
        return getStatus() == Status.PENDING;
    }

    public boolean isArchived() {
        return getStatus() == Status.ARCHIVED;
    }

    public Serializable getContextData(String key) {
        return contextData.get(key);
    }

    public void putContextData(String key, Serializable value) {
        contextData.put(key, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ACE) {
            ACE ace = (ACE) obj;
            // check Calendars without handling timezone
            boolean beginEqual = ace.begin == null && begin == null
                    || !(ace.begin == null || begin == null) && ace.begin.getTimeInMillis() == begin.getTimeInMillis();
            boolean endEqual = ace.end == null && end == null
                    || !(ace.end == null || end == null) && ace.end.getTimeInMillis() == end.getTimeInMillis();
            boolean creatorEqual = ace.creator != null ? ace.creator.equals(creator) : creator == null;
            boolean usernameEqual = ace.username != null ? ace.username.equals(username) : username == null;
            return ace.isGranted == isGranted && usernameEqual && ace.permission.equals(permission) && creatorEqual
                    && beginEqual && endEqual;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + (isGranted ? 1 : 0);
        hash = username != null ? hash * 37 + username.hashCode() : hash;
        hash = creator != null ? hash * 37 + creator.hashCode() : hash;
        hash = begin != null ? hash * 37 + begin.hashCode() : hash;
        hash = end != null ? hash * 37 + end.hashCode() : hash;
        hash = permission != null ? hash * 37 + permission.hashCode() : hash;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append("username=").append(username);
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
        return new ACE(username, permission, isGranted, creator, begin, end, contextData);
    }

    public static ACEBuilder builder(String username, String permission) {
        return new ACEBuilder(username, permission);
    }

    public static class ACEBuilder {

        private String username;

        private String permission;

        private boolean isGranted = true;

        private Calendar begin;

        private Calendar end;

        private String creator;

        private Map<String, Serializable> contextData;

        public ACEBuilder(String username, String permission) {
            this.username = username;
            this.permission = permission;
        }

        public ACEBuilder isGranted(boolean isGranted) {
            this.isGranted = isGranted;
            return this;
        }

        public ACEBuilder begin(Calendar begin) {
            this.begin = begin;
            return this;
        }

        public ACEBuilder end(Calendar end) {
            this.end = end;
            return this;
        }

        public ACEBuilder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public ACEBuilder contextData(Map<String, Serializable> contextData) {
            this.contextData = contextData;
            return this;
        }

        public ACE build() {
            return new ACE(username, permission, isGranted, creator, begin, end, contextData);
        }
    }

}
