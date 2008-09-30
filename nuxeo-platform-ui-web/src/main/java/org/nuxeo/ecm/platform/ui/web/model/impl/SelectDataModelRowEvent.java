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
 * $Id: SelectDataModelRowEvent.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import java.util.EventObject;

import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;

/**
 * SelectModel event to trigger a selection event at the row level.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
// XXX AT: event is triggered at the row level, so maybe the row should carry
// its index within the SelectModel data...
public class SelectDataModelRowEvent extends EventObject {

    private static final long serialVersionUID = -2537709468370440334L;

    private final Boolean selected;

    private final Object data;

    public SelectDataModelRowEvent(SelectDataModelRow source, Boolean selected, Object data) {
        super(source);
        this.selected = selected;
        this.data = data;
    }

    public SelectDataModelRow getRow() {
        return (SelectDataModelRow) getSource();
    }

    public Boolean getSelected() {
        return selected;
    }

    public Object getRowData() {
        return data;
    }

}
