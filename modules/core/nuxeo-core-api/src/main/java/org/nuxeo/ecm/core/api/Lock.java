/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Information about a lock set on a document.
 * <p>
 * The lock information holds the owner, which is a user id, and the lock creation time.
 */
public class Lock implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String owner;

    private final Calendar created;

    private final boolean failed;

    public Lock(String owner, Calendar created, boolean failed) {
        this.owner = owner;
        this.created = created;
        this.failed = failed;
    }

    public Lock(String owner, Calendar created) {
        this(owner, created, false);
    }

    public Lock(Lock lock, boolean failed) {
        this(lock.owner, lock.created, failed);
    }

    /**
     * The owner of the lock.
     *
     * @return the owner, which is a user id
     */
    public String getOwner() {
        return owner;
    }

    /**
     * The creation time of the lock.
     *
     * @return the creation time
     */
    public Calendar getCreated() {
        return created;
    }

    /**
     * The failure state, used for removal results.
     *
     * @return the failure state
     */
    public boolean getFailed() {
        return failed;
    }

    /**
     * @since 11.1
     */
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    /**
     * @since 11.1
     */
    @Override
    public int hashCode() {
        return Objects.hash(owner, created, failed);
    }

    /**
     * @since 11.1
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
