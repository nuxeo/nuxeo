/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;

/**
 * Interface of the pluggable factory used to create a PublishedDocument in a
 * give PublicationTree.
 *
 * @author tiry
 */
public interface PublishedDocumentFactory {

    String getName();

    PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode) throws ClientException;

    PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException;

    void init(CoreSession coreSession, ValidatorsRule validatorsRule, Map<String, String> parameters)
            throws ClientException;

    void init(CoreSession coreSession, Map<String, String> parameters)
            throws ClientException;

    DocumentModel snapshotDocumentBeforePublish(DocumentModel doc)
            throws ClientException;

    PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException;

    /**
     * Computes the list of publishing validators given the document model of
     * the document just published.
     *
     * The string can be prefixed with 'group:' or 'user:'. If there is no
     * prefix (no : in the string) it is assumed to be a user.
     *
     * @param dm a Nuxeo Core document model. (the document that just has been
     *            published)
     * @return a list of principal names.
     * @throws org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException
     */
    String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException;

    /**
     * Returns the registered section validators rule.
     *
     * @return a validators rule
     */
    ValidatorsRule getValidatorsRule() throws PublishingValidatorException;

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be
     *            approved
     * @param comment
     * @throws PublishingException
     */
    void validatorPublishDocument(PublishedDocument publishedDocument,
            String comment) throws ClientException;

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be
     *            rejected
     * @param comment
     * @throws PublishingException
     */
    void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws ClientException;

    boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException;

    boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException;

}
