/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing.api;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author arussel
 *
 */
public interface PublishingService extends Publisher {
    enum DocumentStatus {
        notPublished, waitingValidation, published
    }

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
     * @throws PublishingValidatorException TODO
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
     * Returns the field name used to specify the date at which the publication
     * should occur.
     *
     * @return the field name.
     */
    String getValidDateFieldName();

    /**
     * Returns the schema name where the field used to specify the date at which
     * the publication should occur exists.
     *
     * @return the schema name.
     */
    String getValidDateFieldSchemaPrefixName();

}
