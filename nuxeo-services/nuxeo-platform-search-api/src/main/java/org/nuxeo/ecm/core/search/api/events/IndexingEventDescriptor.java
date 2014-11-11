/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api.events;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
@XObject("indexingEvent")
public class IndexingEventDescriptor implements IndexingEventConf {

    private static final long serialVersionUID = -1655097985665307852L;

    @XNode("@action")
    protected String action;

    @XNodeList(value = "resource", type = HashSet.class, componentType = String.class)
    protected Set<String> resources;

    @XNode("@recursive")
    protected boolean recursive = false;

    @XNode("@name")
    protected String name;

    @XNode("@mode")
    protected String mode = SYNC_ASYNC;


    public IndexingEventDescriptor() {
    }

    public IndexingEventDescriptor(String action, Set<String> resources,
            boolean recursive, String name) {
        this.action = action;
        this.resources = resources;
        this.recursive = recursive;
        this.name = name;
        mode = SYNC_ASYNC;
    }

    public IndexingEventDescriptor(String action, Set<String> resources,
            boolean recursive, String name,String mode) {
        this(action,resources,recursive, name);
        this.mode = mode;
    }

    public String getAction() {
        return action;
    }

    public Set<String> getRelevantResources()  {
        if (resources.isEmpty()) {
            // Apparently, NXRuntime infrastructure never puts null there
            return null;
        }
        return resources;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String getName() {
        return name;
    }

    public String getMode() {
        return mode;
    }

}
