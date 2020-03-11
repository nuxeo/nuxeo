/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
