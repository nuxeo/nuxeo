/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.events;

import java.io.Serializable;
import java.util.Set;

/**
 * Indexing Event Configuration:
 * <p>
 * Stores index related operations that are to be performed for the given
 * event. These operations typically apply to a document that is passed along
 * with the event, although instances of this class don't handle it.</p>
 *
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 *
 */

public interface IndexingEventConf extends Serializable {

    String INDEX = "Index";

    String UN_INDEX = "UnIndex";

    String RE_INDEX = "ReIndex";

    String ONLY_ASYNC ="Asynchronous";

    String ONLY_SYNC ="Synchronous";

    String SYNC_ASYNC ="Both";

    String NEVER ="Never";


    /**
     * Get the action to perform.
     *
     * @return a string
     */
    String getAction();

    /**
     * Must the operation recurse on documents ?
     *
     * @return a boolean
     */
    boolean isRecursive();

    /**
     * Lists the resources that are impacted by the event.
     *
     * <p>This allows to restrict the event to some resources. For instance,
     * the actions that the event signals can require a reindexing of security
     * policies without changing other resources, e.g, the full text, whose
     * indexing can be very costly.
     * </p>
     *
     * @return the set of resource names, as registered against the Search
     * Service, or null to mean all.
     *
     */
    Set<String> getRelevantResources();

    /**
     * Define the mode used to perform the operation
     *
     * ONLY_ASYNC ="Asynchronous"
     * ONLY_SYNC ="Synchronous"
     * SYNC_ASYNC ="Both"
     *
     * @return a String
     */
    String getMode();
}
