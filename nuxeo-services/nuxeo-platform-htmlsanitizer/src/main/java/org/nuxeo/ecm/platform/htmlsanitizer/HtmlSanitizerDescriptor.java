/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("sanitizer")
public class HtmlSanitizerDescriptor {

    @XNode("@name")
    public String name = "";

    @XNode("@enabled")
    public boolean enabled = true;

    // unused
    @XNode("@override")
    public boolean override = false;

    @XNodeList(value = "type", type = ArrayList.class, componentType = String.class)
    public final List<String> types = new ArrayList<String>();

    @XNodeList(value = "field", type = ArrayList.class, componentType = FieldDescriptor.class)
    public final List<FieldDescriptor> fields = new ArrayList<FieldDescriptor>();

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        buf.append(name);
        if (!types.isEmpty()) {
            buf.append(",types=");
            buf.append(types);
        }
        if (!fields.isEmpty()) {
            buf.append(",fields=");
            buf.append(fields);
        }
        buf.append(')');
        return buf.toString();
    }

}
