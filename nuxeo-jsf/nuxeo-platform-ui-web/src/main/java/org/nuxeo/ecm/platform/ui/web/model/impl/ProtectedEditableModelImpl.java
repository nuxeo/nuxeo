/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: ProtectedEditableModelImpl.java 21685 2007-06-30 21:02:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import org.nuxeo.ecm.platform.ui.web.model.EditableModel;
import org.nuxeo.ecm.platform.ui.web.model.ProtectedEditableModel;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class ProtectedEditableModelImpl implements ProtectedEditableModel {

    protected final EditableModel delegate;

    public ProtectedEditableModelImpl(EditableModel delegate) {
        this.delegate = delegate;
    }

    public int getRowCount() {
        return delegate.getRowCount();
    }

    public Object getRowData() {
        return delegate.getRowData();
    }

    public int getRowIndex() {
        return delegate.getRowIndex();
    }

    public void setRowData(Object rowData) {
        delegate.setRowData(rowData);
    }

    public boolean isRowNew() {
        return delegate.isRowNew();
    }

}
