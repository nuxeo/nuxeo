/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import java.io.Serializable;

import org.custommonkey.xmlunit.Diff;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.model.DocumentDiff;

/**
 * Handles a diff between two documents.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DocumentDiffService extends Serializable {

    /**
     * Makes a diff between leftDoc and rightDoc. Returns a DocumentDiff object
     * that wraps the differences, schema by schema and field by field.
     *
     * @param session the session
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @return the document diff
     * @throws ClientException the client exception
     */
    DocumentDiff diff(CoreSession session, DocumentModel leftDoc,
            DocumentModel rightDoc) throws ClientException;

    /**
     * Makes a diff between leftXML and rightXML. Returns a DocumentDiff object
     * that wraps the differences, schema by schema and field by field.
     *
     * @param leftXML the left XML
     * @param rightXML the right XML
     * @return the document diff
     * @throws ClientException the client exception
     */
    DocumentDiff diff(String leftXML, String rightXML) throws ClientException;

    /**
     * Configures XMLUnit.
     */
    void configureXMLUnit();

    /**
     * Configures the diff.
     *
     * @param diff the diff
     */
    void configureDiff(Diff diff);

}
