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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingService.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Publishing service interface.
 *
 * <p>
 * General publishing related service API.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface PublishingService extends Serializable {

    /**
     * Computes the list of publishing validators given the document model of
     * the document just published.
     *
     * @param dm : a Nuxeo Core document model. (the document that just has been
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
