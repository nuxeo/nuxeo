/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: ProtectedEditableModelImpl.java 21685 2007-06-30 21:02:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import javax.el.ValueExpression;

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

    protected final ProtectedEditableModel parent;

    protected final ValueExpression binding;

    public ProtectedEditableModelImpl(String compId, EditableModel delegate, ProtectedEditableModel parent,
            ValueExpression binding) {
        this.componentId = compId;
        this.delegate = delegate;
        this.parent = parent;
        this.binding = binding;
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
        buf.append(" component id=");
        buf.append(componentId);
        buf.append(", binding=");
        buf.append(binding);
        buf.append(", delegate=");
        buf.append(delegate);
        buf.append(", parent=");
        buf.append(parent);
        buf.append('}');
        return buf.toString();
    }

}
