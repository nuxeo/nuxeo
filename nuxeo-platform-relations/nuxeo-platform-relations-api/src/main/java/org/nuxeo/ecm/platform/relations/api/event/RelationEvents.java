/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: RelationEvents.java 22537 2007-07-13 16:07:30Z gracinet $
 */

package org.nuxeo.ecm.platform.relations.api.event;

/**
 * Relation event types.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class RelationEvents {

    // event ids

    public static final String BEFORE_RELATION_CREATION = "beforeRelationCreation";
    public static final String AFTER_RELATION_CREATION = "afterRelationCreation";
    public static final String BEFORE_RELATION_MODIFICATION = "beforeRelationModification";
    public static final String AFTER_RELATION_MODIFICATION = "afterRelationModification";
    public static final String BEFORE_RELATION_REMOVAL = "beforeRelationRemoval";
    public static final String AFTER_RELATION_REMOVAL = "afterRelationRemoval";

    // event keys constants

    public static final String GRAPH_NAME_EVENT_KEY = "graph";
    public static final String STATEMENTS_EVENT_KEY = "statements";
    public static final String CATEGORY = "relationNotificationCategory";

    private RelationEvents() {
    }

}
