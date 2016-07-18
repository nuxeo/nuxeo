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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @deprecated Since 8.4. See uf of 'soffice' with {@link org.nuxeo.ecm.platform.convert.plugins.CommandLineConverter}
 *             instead
 */
@Deprecated
@XObject("OOoManager")
public class OOoManagerDescriptor {

    @XNode("@enabled")
    public boolean enabled;

    @XNodeList(value = "portNumbers/portNumber", type = ArrayList.class, componentType = Integer.class)
    public List<Integer> portNumbers;

    @XNodeList(value = "pipeNames/pipeName", type = ArrayList.class, componentType = String.class)
    public List<String> pipeNames;

    public boolean isEnabled() {
        return enabled;
    }

    public int[] getPortNumbers() {
        if (portNumbers != null) {
            int[] ports = new int[portNumbers.size()];
            for (int i = 0; i < portNumbers.size(); i++) {
                ports[i] = portNumbers.get(i);
            }
            return ports;
        } else {
            return null;
        }
    }

    public String[] getPipeNames() {
        if (pipeNames != null) {
            String[] pipes = new String[pipeNames.size()];
            pipeNames.toArray(pipes);
            return pipes;
        } else {
            return null;
        }
    }

}
