/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
