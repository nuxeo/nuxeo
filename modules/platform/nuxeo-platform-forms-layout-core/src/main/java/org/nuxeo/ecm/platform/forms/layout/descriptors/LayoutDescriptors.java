/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Descriptor for a list of layout definitions
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("layouts")
public class LayoutDescriptors {

    @XNodeList(value = "layout", type = ArrayList.class, componentType = LayoutDescriptor.class)
    List<LayoutDescriptor> layouts;

    public List<LayoutDefinition> getLayouts() {
        if (layouts == null) {
            return null;
        }
        List<LayoutDefinition> res = new ArrayList<>();
        for (LayoutDescriptor item : layouts) {
            res.add(item.getLayoutDefinition());
        }
        return res;
    }

}
