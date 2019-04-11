/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public final List<String> types = new ArrayList<>();

    @XNodeList(value = "field", type = ArrayList.class, componentType = FieldDescriptor.class)
    public final List<FieldDescriptor> fields = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append(name);
        if (!types.isEmpty()) {
            sb.append(",types=");
            sb.append(types);
        }
        if (!fields.isEmpty()) {
            sb.append(",fields=");
            sb.append(fields);
        }
        sb.append(')');
        return sb.toString();
    }

}
