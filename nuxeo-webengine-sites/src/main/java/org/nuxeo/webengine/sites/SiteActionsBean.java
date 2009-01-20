/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.webengine.sites;

import javax.faces.component.UISelectBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Performs re-rendering of webcontainer layout widgets.
 *
 * @author Anahide Tchertchian
 */
public class SiteActionsBean {

    private static final Log log = LogFactory.getLog(SiteActionsBean.class);

    public static final String SCHEMA_NAME = "webcontainer";

    public static final String ISWEBCONTAINER_PROPERTY_NAME = "isWebContainer";

    protected UISelectBoolean checkboxComponent;

    public UISelectBoolean getCheckboxComponent() {
        return checkboxComponent;
    }

    public void setCheckboxComponent(UISelectBoolean checkboxComponent) {
        this.checkboxComponent = checkboxComponent;
    }

    public boolean isWebContainerChecked() {
        Boolean checked = false;
        if (checkboxComponent != null) {
            UISelectBoolean checkbox = checkboxComponent;
            Object currentValue = checkbox.getSubmittedValue();
            if (currentValue == null) {
                currentValue = checkbox.getValue();
            }
            if (currentValue != null) {
                checked = Boolean.valueOf(currentValue.toString());
            }
        }
        return checked;
    }

}
