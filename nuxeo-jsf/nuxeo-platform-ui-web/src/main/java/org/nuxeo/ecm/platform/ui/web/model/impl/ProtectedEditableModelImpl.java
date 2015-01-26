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

import javax.el.ValueExpression;

import org.nuxeo.ecm.platform.ui.web.model.EditableModel;
import org.nuxeo.ecm.platform.ui.web.model.ProtectedEditableModel;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ProtectedEditableModelImpl implements ProtectedEditableModel {

    protected final EditableModel delegate;

    protected final ProtectedEditableModel parent;

    protected final ValueExpression binding;

    public ProtectedEditableModelImpl(EditableModel delegate, ProtectedEditableModel parent, ValueExpression binding) {
        this.delegate = delegate;
        this.parent = parent;
        this.binding = binding;
    }

    @Override
    public int getRowCount() {
        return delegate.getRowCount();
    }

    @Override
    public Object getRowData() {
        return delegate.getRowData();
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
    public ValueExpression getBinding() {
        return binding;
    }

    @Override
    public ProtectedEditableModel getParent() {
        return parent;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(ProtectedEditableModelImpl.class.getSimpleName());
        buf.append(" {");
        buf.append(" binding=");
        buf.append(binding);
        buf.append(", delegate=");
        buf.append(delegate);
        buf.append(", parent=");
        buf.append(parent);
        buf.append('}');
        return buf.toString();
    }

}
