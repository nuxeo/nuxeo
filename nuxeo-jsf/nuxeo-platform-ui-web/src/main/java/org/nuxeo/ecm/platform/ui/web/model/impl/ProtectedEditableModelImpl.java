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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.model.EditableModel;
import org.nuxeo.ecm.platform.ui.web.model.ProtectedEditableModel;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ProtectedEditableModelImpl implements ProtectedEditableModel {

    private static final Log log = LogFactory.getLog(ProtectedEditableModelImpl.class);

    protected final String componentId;

    protected final EditableModel delegate;

    public ProtectedEditableModelImpl(String compId, EditableModel delegate) {
        this.componentId = compId;
        this.delegate = delegate;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    @Override
    public int getRowCount() {
        return delegate.getRowCount();
    }

    @Override
    public Object getRowData() {
        Object data = delegate.getRowData();
        if (log.isDebugEnabled()) {
            log.debug("getRowData " + getComponentId() + " -> " + data);
        }
        return data;
    }

    @Override
    public int getRowIndex() {
        return delegate.getRowIndex();
    }

    @Override
    public void setRowData(Object rowData) {
        delegate.setRowData(rowData);
    }

    @Override
    public boolean isRowNew() {
        return delegate.isRowNew();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(ProtectedEditableModelImpl.class.getSimpleName());
        buf.append(" {");
        buf.append(" component id=");
        buf.append(componentId);
        buf.append(", delegate=");
        buf.append(delegate);
        buf.append('}');
        return buf.toString();
    }

}
