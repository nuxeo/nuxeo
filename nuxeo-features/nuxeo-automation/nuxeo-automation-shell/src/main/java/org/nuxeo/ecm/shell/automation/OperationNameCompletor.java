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
 *     bstefanescu
 */
package org.nuxeo.ecm.shell.automation;

import jline.SimpleCompletor;

import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OperationNameCompletor extends SimpleCompletor {

    protected RemoteContext ctx;

    public OperationNameCompletor() {
        this(Shell.get().getContextObject(RemoteContext.class));
    }

    public OperationNameCompletor(RemoteContext ctx) {
        super (ctx.getSession().getOperations().keySet().toArray(new String[0]));
    }
}
