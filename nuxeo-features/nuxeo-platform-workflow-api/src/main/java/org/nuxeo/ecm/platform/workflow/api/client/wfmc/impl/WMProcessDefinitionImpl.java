/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: WMProcessDefinitionImpl.java 21821 2007-07-03 08:59:32Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;

/**
 * Process definition implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMProcessDefinitionImpl implements WMProcessDefinition {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected int version;

    protected String name;

    public WMProcessDefinitionImpl() {
    }

    public WMProcessDefinitionImpl(String id, int version, String name) {
        this.id = id;
        this.version = version;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

}
