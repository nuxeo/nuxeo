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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 *
 */
@XObject("field")
public class DisplayedFieldsDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@label")
    private String label;

    @XNode("@displayed")
    private boolean displayed = false;

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDisplayed() {
        return displayed;
    }

}
