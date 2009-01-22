/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class SelectManyListboxComponent extends DirectoryAwareComponent {

    public static final String COMPONENT_TYPE = "nxdirectory.selectManyListbox";

    public static final String COMPONENT_FAMILY = "nxdirectory.selectManyListbox";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SelectManyListboxComponent.class);

    private String displayValueOnlySeparator;

    public SelectManyListboxComponent() {
        setRendererType(COMPONENT_TYPE);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        displayValueOnlySeparator = (String) values[1];
    }

    @Override
    public Object saveState(FacesContext arg0) {
        Object[] values = new Object[2];
        values[0] = super.saveState(arg0);
        values[1] = displayValueOnlySeparator;
        return values;
    }

    public String getDisplayValueOnlySeparator() {
        return displayValueOnlySeparator;
    }

    public void setDisplayValueOnlySeparator(String displayValueOnlySeparator) {
        this.displayValueOnlySeparator = displayValueOnlySeparator;
    }

}
