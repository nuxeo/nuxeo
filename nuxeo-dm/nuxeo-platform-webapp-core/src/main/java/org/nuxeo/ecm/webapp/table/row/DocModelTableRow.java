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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.table.row;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;

/**
 * A table row DocumentModel aware. Keeps a reference with a DocumentModel
 * associated with the row. This helps identify the table row starting from a
 * document model. Useful for opertions on the table model rows like delete etc.
 * Also keeps a list of cells associated with the row.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class DocModelTableRow extends TableRow {

    private static final long serialVersionUID = -3789350932940988658L;

    private static final Log log = LogFactory.getLog(DocModelTableRow.class);

    protected DocumentModel docModel;

    protected DocModelTableRow() throws ClientException {
    }

    public DocModelTableRow(DocumentModel document,
            List<AbstractTableCell> cells) throws ClientException {
        super(cells);

        if (null == document) {
            throw new ClientException("Null document received.");
        }

        docModel = document;

        log.debug("Constructed with document (title): "
                + document.getProperty("dublincore", "title"));
    }

    public DocumentModel getDocModel() {
        return docModel;
    }

    public void setDocModel(DocumentModel docModel) {
        this.docModel = docModel;
    }

    /**
     * Implements equality by checking the equality on the document references.
     * As a side note also the row id could be used as checking the document
     * references basically prevents the programmer to use the same document for
     * more than one row.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DocModelTableRow)) {
            return false;
        }
        DocModelTableRow row = (DocModelTableRow) other;

        return docModel.getRef().equals(row.docModel.getRef());
    }

    @Override
    public int hashCode() {
        return docModel.getRef().hashCode();
    }

}
