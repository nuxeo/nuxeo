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
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Thierry Delprat
 */

@XObject("groupComputerChain")
public class GroupComputerChainDescriptor {

    @XNodeList(value = "computers/computer", type = ArrayList.class, componentType = String.class)
    private List<String> computerNames;

    @XNode("@append")
    private boolean append = false;

    public boolean isAppend() {
        return append;
    }

    public List<String> getComputerNames() {
        if (computerNames != null) {
            return computerNames;
        } else {
            return new ArrayList<>();
        }
    }

}
