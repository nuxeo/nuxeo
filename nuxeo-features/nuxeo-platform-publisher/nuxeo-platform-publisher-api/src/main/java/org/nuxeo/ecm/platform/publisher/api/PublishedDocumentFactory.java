/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;

/**
 * Interface of the pluggable factory used to create a PublishedDocument in a give PublicationTree.
 *
 * @author tiry
 */
public interface PublishedDocumentFactory {

    String getName();

    PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode);

    PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params);

    void init(CoreSession coreSession, ValidatorsRule validatorsRule, Map<String, String> parameters);

    void init(CoreSession coreSession, Map<String, String> parameters);

    DocumentModel snapshotDocumentBeforePublish(DocumentModel doc);

    PublishedDocument wrapDocumentModel(DocumentModel doc);

    /**
     * Computes the list of publishing validators given the document model of the document just published. The string
     * can be prefixed with 'group:' or 'user:'. If there is no prefix (no : in the string) it is assumed to be a user.
     *
     * @param dm a Nuxeo Core document model. (the document that just has been published)
     * @return a list of principal names.
     */
    String[] getValidatorsFor(DocumentModel dm);

    /**
     * Returns the registered section validators rule.
     *
     * @return a validators rule
     */
    ValidatorsRule getValidatorsRule();

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be approved
     * @param comment
     */
    void validatorPublishDocument(PublishedDocument publishedDocument, String comment);

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be rejected
     * @param comment
     */
    void validatorRejectPublication(PublishedDocument publishedDocument, String comment);

    boolean hasValidationTask(PublishedDocument publishedDocument);

    boolean canManagePublishing(PublishedDocument publishedDocument);

}
