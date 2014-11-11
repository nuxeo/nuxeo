/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("OOoManager")
public class OOoManagerDescriptor {

    @XNodeList(value = "portNumbers/portNumber", type = ArrayList.class, componentType = Integer.class)
    public List<Integer> portNumbers;

    @XNodeList(value = "pipeNames/pipeName", type = ArrayList.class, componentType = String.class)
    public List<String> pipeNames;

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
