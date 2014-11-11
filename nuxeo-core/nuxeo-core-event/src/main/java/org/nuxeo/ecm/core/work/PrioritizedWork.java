/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

/**
 * A {@link Work} instance designed to be inserted in a {@link WorkManager}
 * queue configured to use a priority queue (through {@code usePriority}).
 *
 * @since 5.7
 */
public abstract class PrioritizedWork extends AbstractWork implements
        Comparable<PrioritizedWork> {

    protected final String identity;

    /**
     * Constructs a prioritized {@link Work} instance, whose priority will be
     * based on the ordering of the {@code identity} string compared to other
     * {@link PrioritizedWork} instances.
     *
     * @param identity the value through which priority is computed
     */
    protected PrioritizedWork(String identity) {
        if (identity == null) {
            throw new NullPointerException("Identity cannot be null");
        }
        this.identity = identity;
    }

    @Override
    public int compareTo(PrioritizedWork other) {
        return identity.compareTo(other.identity);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof PrioritizedWork)) {
            return false;
        }
        return identity.equals(((PrioritizedWork) other).identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

}
