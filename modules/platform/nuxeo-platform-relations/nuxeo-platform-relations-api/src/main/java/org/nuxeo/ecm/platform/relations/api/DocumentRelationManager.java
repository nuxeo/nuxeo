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
 *     Remi Cattiau
 */
package org.nuxeo.ecm.platform.relations.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;

/**
 * Create relations between documents
 *
 * @since 5.9.2
 */
public interface DocumentRelationManager {

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate, boolean inverse);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @param includeStatementsInEvents will add the statement to the events RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse,
            boolean includeStatementsInEvents);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @param includeStatementsInEvents will add the statement to the events RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @param comment of the relation
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment);

    /**
     * @param from document
     * @param to document
     * @param predicate relation type
     */
    void deleteRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate);

    /**
     * @param statement to delete
     */
    void deleteRelation(CoreSession session, Statement statement);

    /**
     * @param stmt to delete
     * @param includeStatementsInEvents add the current statement in event RelationEvents.BEFORE_RELATION_REMOVAL and
     *            RelationEvents.AFTER_RELATION_REMOVAL
     */
    void deleteRelation(CoreSession session, Statement stmt, boolean includeStatementsInEvents);

    /**
     * @param from document
     * @param to document
     * @param predicate relation type
     * @param includeStatementsInEvents add the current statement in event RelationEvents.BEFORE_RELATION_REMOVAL and
     *            RelationEvents.AFTER_RELATION_REMOVAL
     */
    void deleteRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate,
            boolean includeStatementsInEvents);

}
