/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

/**
 * Constant related to Core Events sent by VCS.
 */
public class EventConstants {

    /**
     * The event type for VCS invalidations. The event context contains the
     * repository name, and some properties describing the invalidations
     * themselves.
     *
     * @see #INVAL_MODIFIED_DOC_IDS
     * @see #INVAL_MODIFIED_PARENT_IDS
     * @see #INVAL_LOCAL
     */
    public static final String EVENT_VCS_INVALIDATIONS = "vcsInvalidations";

    /**
     * Invalidation event: event context property for a {@code Set<String>} of
     * modified document ids.
     */
    public static final String INVAL_MODIFIED_DOC_IDS = "modifiedDocIds";

    /**
     * Invalidation event: event context property for a {@code Set<String>} of
     * modified parent ids (which means that the list of children for this
     * parent has changed). May contain spurious ids that do not correspond to
     * parents.
     */
    public static final String INVAL_MODIFIED_PARENT_IDS = "modifiedParentIds";

    /**
     * Invalidation event: event context property for a {@code Boolean}
     * specifying whether the invalidations come from this cluster node ({@code
     * true}), or if they come from a remote cluster node ({@code false}).
     */
    public static final String INVAL_LOCAL = "local";

    private EventConstants() {
        // utility class
    }

}
