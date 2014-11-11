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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * Fake action handler for tests.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class FakeJbpmWorkflowActionHandler extends JbpmWorkflowActionHandler {

    // Before each test (in the setUp), the isExecuted member
    // will be set to false.
    // Package-visible because it is accessed directly from the tests.
    static boolean isExecuted = false;

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
        System.out.println("###############################################");
        isExecuted = true;
    }

}
