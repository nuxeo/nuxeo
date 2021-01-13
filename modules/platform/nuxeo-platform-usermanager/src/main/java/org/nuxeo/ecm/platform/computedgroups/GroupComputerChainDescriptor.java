/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;

/**
 * @author Thierry Delprat
 */

@XObject("groupComputerChain")
@XRegistry
public class GroupComputerChainDescriptor {

    @XNodeList(value = "computers/computer", type = ArrayList.class, componentType = String.class)
    @XMerge(value = XMerge.MERGE, fallback = "@append", defaultAssignment = false)
    private List<String> computerNames;

    public List<String> getComputerNames() {
        return Collections.unmodifiableList(computerNames);
    }

}
