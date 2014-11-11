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
 * $Id: SelectDataModelRowImpl.java 28463 2008-01-03 18:02:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;

/**
 * SelectModelRow that has methods to trigger selection events to its
 * SelectModel.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
// XXX AT: maybe should carry its index within the SelectModel data.
public class SelectDataModelRowImpl implements SelectDataModelRow {

    private final Log log = LogFactory.getLog(SelectDataModelImpl.class);

    private final SelectDataModel model;

    private Boolean selected;

    private Object data;

    public SelectDataModelRowImpl(SelectDataModel model, Boolean selected,
            Object data) {
        this.model = model;
        this.selected = selected;
        this.data = data;
    }

    public SelectDataModel getSelectModel() {
        return model;
    }

    public void selectionChanged(ValueChangeEvent event) {
        if (model == null) {
            log.error("Could not send selection event: SelectModel is null");
            return;
        }
        Object newValue = event.getNewValue();
        Object oldValue = event.getOldValue();
        // XXX AT: no need to change the selected attribute since it is supposed
        // to be a value binding of the select checkbox, like this listener is
        // supposed to be a value change listener of the select checkbox
        if (newValue instanceof Boolean && oldValue instanceof Boolean
                && !newValue.equals(oldValue)) {
            Boolean selection = (Boolean) newValue;
            SelectDataModelRowEvent selectEvent = new SelectDataModelRowEvent(
                    this, selection, data);

            for (SelectDataModelListener listener
                    : model.getSelectModelListeners()) {

                Object seamComponent = SeamComponentCallHelper
                        .getSeamComponentByRef(listener);
                if (seamComponent == null) {
                    // this is not a Seam component : direct call
                    listener.processSelectRowEvent(selectEvent);
                } else {
                    // this is a Seam component : use the wrapper
                    SelectDataModelListener wrappedListener = (SelectDataModelListener) seamComponent;
                    wrappedListener.processSelectRowEvent(selectEvent);
                }

                // alternative call solution via reflection
                // SeamComponentCallHelper.callSeamComponentByRef(listener,
                // "processSelectRowEvent", selectEvent);
            }
        }
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
