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

package org.nuxeo.ecm.webapp.table.model;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.table.header.TableColHeader;
import org.nuxeo.ecm.webapp.table.row.DocModelTableRow;
import org.nuxeo.ecm.webapp.table.row.TableRow;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class DocModelTableModel extends TableModel {
    private static final long serialVersionUID = -2063444397095908727L;

    private static final Log log = LogFactory.getLog(DocModelTableModel.class);

    protected DocumentModel selectedDocModel;

    protected DocModelTableModelVisitor visitor;

    public DocModelTableModel(List<TableColHeader> columnHeaders,
            List<TableRow> data) throws ClientException {
        super(columnHeaders, data);
    }

    /**
     * @return Returns the selectedDocModel.
     */
    public DocumentModel getSelectedDocModel() {
        return selectedDocModel;
    }

    /**
     * @param selectedDocModel
     *            The selectedDocModel to set.
     */
    public void setSelectedDocModel(DocumentModel selectedDocModel) {
        this.selectedDocModel = selectedDocModel;
    }

    @Override
    public void verifyRowConsistency(TableRow row) throws ClientException {
        super.verifyRowConsistency(row);

        if (row instanceof DocModelTableRow) {
        } else {
            throw new ClientException(
                    "Found row that is not a DocModelTableRow instance.");
        }
    }

    @Override
    public void process(ActionEvent event) {
        super.process(event);

        if (data.isRowAvailable() && columnHeaders.isRowAvailable()) {
            DocModelTableRow row = (DocModelTableRow) getCurrentRow();

            selectedDocModel = row.getDocModel();
        }
    }

    public List<DocumentModel> getSelectedDocs() throws ClientException {
        List<DocumentModel> selectedDocs = new ArrayList<DocumentModel>();
        List<TableRow> selectedRows = getSelectedRows();

        for (TableRow selectedRow : selectedRows) {
            DocumentModel docModel = ((DocModelTableRow) selectedRow).getDocModel();
            selectedDocs.add(docModel);
        }

        return selectedDocs;
    }

    /**
     *
     * @param doc
     * @return true if the document was successfuly removed from this structure
     * @throws ClientException
     */
    public boolean removeRow(DocumentModel doc) throws ClientException {
        if (null == doc) {
            throw new ClientException("Null param received.");
        }

        @SuppressWarnings("unchecked")
        List<DocModelTableRow> rows = (List<DocModelTableRow>) getData()
                .getWrappedData();
        DocModelTableRow rowToBeRemoved = null;

        for (DocModelTableRow row : rows) {
            if (row.getDocModel().getRef().equals(doc.getRef())) {
                rowToBeRemoved = row;
                break;
            }
        }

        if (null != rowToBeRemoved) {
            return rows.remove(rowToBeRemoved);
        }

        return false;
    }

    public DocModelTableModelVisitor getVisitor() {
        return visitor;
    }

    public void setVisitor(DocModelTableModelVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Accept method. Calls visit method from the visitor inside if there is a
     * visitor available.<br>
     * Called when a new document needs to be added to the table model.
     *
     * @param doc
     * @throws ClientException
     */
    public void addRow(DocumentModel doc) throws ClientException {
        if (null != visitor) {
            addRow(visitor.createDocModelTableModelRow(doc));
        }
    }

    // --------------------------------------------------------
    // CacheListener intf implementation - start
    public void documentRemove(DocumentModel docModel) {
        try {
            boolean removed = removeRow(docModel);
            log.debug("<documentRemove> removed: " + removed);
        } catch (ClientException e) {
            // TODO: more robust exception handling?
            log.error(e);
        }
    }

    public void documentRemoved(String fqn) {
        // TODO Auto-generated method stub
    }

    public void documentUpdate(DocumentModel docModel, boolean pre) {
        // TODO Auto-generated method stub
    }
    // CacheListener intf implementation - end
    // --------------------------------------------------------
}
