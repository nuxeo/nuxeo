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
 * $Id: ProtectedEditableModel.java 27477 2007-11-20 19:55:44Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import javax.el.ValueExpression;
import javax.faces.model.DataModel;

/**
 * Interface for protected editable data model.
 * <p>
 * Used to expose only some methods of {@link EditableModel} to user interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface ProtectedEditableModel {

    /**
     * Returns the originating JSF component id this model is attached to.
     * <p>
     * Useful to debug model exposure to the context.
     *
     * @since 8.1
     */
    String getComponentId();

    /**
     * @see DataModel#getRowCount()
     */
    int getRowCount();

    /**
     * @see DataModel#getRowData()
     */
    Object getRowData();

    /**
     * Sets row data using given value.
     */
    void setRowData(Object data);

    /**
     * @see DataModel#getRowIndex()
     */
    int getRowIndex();

    /**
     * @see EditableModel#isRowNew()
     */
    boolean isRowNew();

    /**
     * @since 7.2
     */
    ValueExpression getBinding();

    /**
     * @since 7.2
     */
    ProtectedEditableModel getParent();

}
