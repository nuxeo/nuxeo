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
 * $Id: ProtectedEditableModel.java 27477 2007-11-20 19:55:44Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.model;

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

}
