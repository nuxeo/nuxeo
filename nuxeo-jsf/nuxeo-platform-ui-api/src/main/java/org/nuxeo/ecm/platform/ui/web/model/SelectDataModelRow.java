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
 * $Id: SelectDataModelRow.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import javax.faces.event.ValueChangeEvent;

/**
 * Used in SelectModel to represent a row and handle its selection/unselection.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public interface SelectDataModelRow {

    SelectDataModel getSelectModel();

    /**
     * Value change listener that can be bound to any jsf component (usually a
     * checkbox).
     * <p>
     * This is supposed to trigger SelectModelListener methods declared on the
     * SelectModel.
     */
    void selectionChanged(ValueChangeEvent event);

    void setSelected(Boolean selected);

    Boolean getSelected();

    Object getData();

    void setData(Object data);

}
