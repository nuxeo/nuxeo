/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Type view to display a given document sub-type.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
@XObject("type")
public class SubType implements Serializable {

    private static final long serialVersionUID = 1L;

    List<String> hidden;

    @XNode
    String name;

    public List<String> getHidden() {
        if (hidden == null) {
            hidden = new ArrayList<String>();
        }
        return hidden;
    }

    @XNode("@hidden")
    public void setHidden(String value) {
        String[] hiddenCases = value.split("(\\s+)(?=[^,])|(\\s*,\\s*)");
        hidden = new ArrayList<String>(Arrays.asList(hiddenCases));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
