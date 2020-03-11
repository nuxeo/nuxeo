/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations;

import java.io.IOException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.scripting.Scripting.GroovyScript;
import org.nuxeo.ecm.automation.core.scripting.Scripting.MvelScript;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

/**
 * Run a script given as the input of the operation (as a blob). Note that this operation is available only as
 * administrator
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RunInputScript.ID, category = Constants.CAT_SCRIPTING, label = "Run Input Script", description = "Run a script from the input blob. A blob comtaining script result is returned.", aliases = { "Context.RunInputScript" })
public class RunInputScript {

    public static final String ID = "RunInputScript";

    @Context
    protected OperationContext ctx;

    @Param(name = "type", required = false, values = { "mvel", "groovy" }, widget = Constants.W_OPTION)
    protected String type = "mvel";

    @OperationMethod
    public Blob run(Blob blob) throws OperationException, IOException {
        if (!ctx.getPrincipal().isAdministrator()) {
            throw new OperationException("Not allowed. You must be administrator to run scripts");
        }
        Object r = null;
        if (type.equals("mvel")) {
            r = MvelScript.compile(blob.getString()).eval(ctx);
        } else if (type.equals("groovy")) {
            r = new GroovyScript(blob.getString()).eval(ctx);
        } else {
            throw new OperationException("Unknown scripting language " + type);
        }
        if (r != null) {
            Blob b = Blobs.createBlob(r.toString());
            b.setFilename("result");
            return b;
        }
        return null;
    }

}
