/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: PropertyListDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.actions;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Action property list descriptor
 *
 * @since 5.6
 */
@XObject("propertyList")
public class ActionPropertyListDescriptor {

    @XNodeList(value = "value", type = String[].class, componentType = String.class)
    String[] values = new String[0];

    public String[] getValues() {
        return values;
    }

    @Override
    public ActionPropertyListDescriptor clone() {
        ActionPropertyListDescriptor clone = new ActionPropertyListDescriptor();
        if (values != null) {
            clone.values = values.clone();
        }
        return clone;
    }

}
