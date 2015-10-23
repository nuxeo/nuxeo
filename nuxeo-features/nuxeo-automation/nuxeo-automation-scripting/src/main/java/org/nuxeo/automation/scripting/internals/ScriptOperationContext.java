/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;

/**
 * This operation context wrapper is used for Automation Scripting to avoid disposing traces, files and login stack and
 * let the usage of such operation as {@link org.nuxeo.ecm.automation.core.operations.login.LoginAs}
 *
 * @since 7.10
 */
public class ScriptOperationContext extends OperationContext {

    public ScriptOperationContext() {
        super();
    }

    public ScriptOperationContext(OperationContext ctx) {
        super(ctx);
    }

    @Override
    public void dispose() {
        // do nothing
    }

    public void deferredDispose() throws OperationException {
        super.dispose();
    }
}
