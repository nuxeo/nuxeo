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
import javax.faces.model.ListDataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.table.cell.UserTableCell;
import org.nuxeo.ecm.webapp.table.header.TableColHeader;
import org.nuxeo.ecm.webapp.table.row.TableRow;
import org.nuxeo.ecm.webapp.table.row.UserPermissionsTableRow;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class UserPermissionsTableModel extends TableModel {

    private static final long serialVersionUID = -2063444397095908728L;

    private static final Log log = LogFactory.getLog(UserPermissionsTableModel.class);

    protected String selectedUser;

    protected UserPermissionsTableModelVisitor visitor;

    public UserPermissionsTableModel(List<TableColHeader> columnHeaders,
            List<UserPermissionsTableRow> data) throws ClientException {
        if (null == data || null == columnHeaders) {
            throw new ClientException("Received data is inconsistent.");
        }

        setData(new ListDataModel(data));
        setColumnHeaders(new ListDataModel(columnHeaders));

        selectedRowIdentifiers = new ArrayList<Comparable>();

        log.debug("constructed...");
    }

    /**
     * @return Returns the selectedUser.
     */
    public String getSelectedUser() {
        return selectedUser;
    }

    /**
     * @param selectedUser
     *            The selectedUser to set.
     */
    public void setSelectedUser(String selectedUser) {
        this.selectedUser = selectedUser;
    }

    @Override
    public void process(ActionEvent event) {
        super.process(event);

        if (data.isRowAvailable() && columnHeaders.isRowAvailable()) {
            UserPermissionsTableRow row = (UserPermissionsTableRow) getCurrentRow();

            setSelectedUser(row.getUser());
        }
    }

    public List<String> getSelectedUsers() throws ClientException {
        List<String> selectedUsers = new ArrayList<String>();
        List<TableRow> selectedRows = getSelectedRows();

        for (TableRow selectedRow : selectedRows) {
            String user = ((UserPermissionsTableRow) selectedRow).getUser();
            selectedUsers.add(user);
        }

        return selectedUsers;
    }

    public void removeRow(String user) throws ClientException {
        if (null == user) {
            throw new ClientException("Null param received.");
        }

        @SuppressWarnings("unchecked")
        List<UserPermissionsTableRow> rows = (List<UserPermissionsTableRow>) getData()
                .getWrappedData();
        UserPermissionsTableRow rowToBeRemoved = null;

        for (UserPermissionsTableRow row : rows) {
            if (row.getUser().equals(user)) {
                rowToBeRemoved = row;
                break;
            }
        }

        if (null != rowToBeRemoved) {
            rows.remove(rowToBeRemoved);
        }
    }

    public UserPermissionsTableModelVisitor getVisitor() {
        return visitor;
    }

    public void setVisitor(UserPermissionsTableModelVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Accept method. Calls visit method from the visitor inside if there is a
     * visitor available.<p>
     * Called when a new user needs to be added to the table model.
     *
     * @param user
     * @throws ClientException
     */
    public void addRow(String user) throws ClientException {
        if (null != getVisitor()) {
            addRow(getVisitor().createDocModelTableModelRow(user));
        }
    }

    public String getCurrentUserType() {
        UserTableCell cell = (UserTableCell) getCurrentCell();

        if (null != cell) {
            return cell.getType();
        } else {
            log.error("No current cell.");
            return null;
        }
    }
}
