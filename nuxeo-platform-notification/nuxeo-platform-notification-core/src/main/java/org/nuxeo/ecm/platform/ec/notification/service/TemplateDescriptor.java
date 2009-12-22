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

package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
@XObject("template")
public class TemplateDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@src")
    protected String src;

    // this is set by the type service to the context that knows how to locate
    // the schema file
    private RuntimeContext context;


    public TemplateDescriptor() {
    }

    public TemplateDescriptor(String name) {
        this.name = name;
    }

    public RuntimeContext getContext() {
        return context;
    }

    public void setContext(RuntimeContext context) {
        this.context = context;
    }

}
