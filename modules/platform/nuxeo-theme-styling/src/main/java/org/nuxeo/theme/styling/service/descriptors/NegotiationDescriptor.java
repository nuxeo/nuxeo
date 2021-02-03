/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor for contributed negotiators.
 *
 * @since 7.4
 */
@XObject("negotiation")
@XRegistry
public class NegotiationDescriptor {

    @XNode("@target")
    @XRegistryId
    protected String target;

    @XNodeList(value = "negotiator", type = ArrayList.class, componentType = NegotiatorDescriptor.class)
    @XMerge(value = XMerge.MERGE, fallback = "@append", defaultAssignment = false)
    protected List<NegotiatorDescriptor> negotiators;

    public String getTarget() {
        return target;
    }

    public List<NegotiatorDescriptor> getNegotiators() {
        List<NegotiatorDescriptor> res = new ArrayList<>();
        if (negotiators != null) {
            res.addAll(negotiators);
        }
        Collections.sort(res);
        return res;
    }

}
