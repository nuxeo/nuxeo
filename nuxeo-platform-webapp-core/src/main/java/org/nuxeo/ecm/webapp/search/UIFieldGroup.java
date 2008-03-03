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
 *     george
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.types.FieldWidget;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated remove in 5.2
 */
@Deprecated
public class UIFieldGroup extends ArrayList<FieldWidget>
        implements Serializable {

    private static final long serialVersionUID = 1622436322479811069L;

    private List<FieldWidget> fieldWidgets;

    private String name;

    private String label;

    public UIFieldGroup(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public List<FieldWidget> getFieldWidgets() {
        return fieldWidgets;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
