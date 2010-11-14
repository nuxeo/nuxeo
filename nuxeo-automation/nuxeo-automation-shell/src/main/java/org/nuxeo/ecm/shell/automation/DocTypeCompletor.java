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

import java.util.List;

import jline.SimpleCompletor;

import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DocTypeCompletor extends SimpleCompletor {

    protected RemoteContext ctx;

    public DocTypeCompletor() {
        this(Shell.get().getContextObject(RemoteContext.class));
    }

    public DocTypeCompletor(RemoteContext ctx) {
        super(new String[0]);
        this.ctx = ctx;
    }

    @SuppressWarnings("rawtypes")
    public int complete(String buffer, int cursor, List clist) {
        String[] names = new String[] { "Workspace", "Section", "Folder",
                "File", "Note" };
        setCandidateStrings(names);
        return super.complete(buffer, cursor, clist);
    }

}
