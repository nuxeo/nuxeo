/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.node;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author arussel
 *
 */
public class ForeachFork implements ActionHandler {

    private String list;

    private String var;

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public void execute(ExecutionContext executionContext) {
        assert list != null;
        assert var != null;
        List l = (List) executionContext.getContextInstance().getTransientVariable(
                list);
        if (l == null) {
            l = (List) executionContext.getVariable(list);
        }
        executionContext.getToken();
        for (Object obj : l) {
            Token childToken = new Token(executionContext.getToken(),
                    obj.toString());
            executionContext.getContextInstance().setTransientVariable(var, obj);
            childToken.signal();
        }
    }
}
