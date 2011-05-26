/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.execution;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Run an embedded operation chain that returns a Blob using the
 * current input.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RunFileChain.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run File Chain", description = "Run an operation chain which is returning a file in the current context. The input for the chain to run is a file or a list of files. Return the output of the chain as a file or a list of files.")
public class RunFileChain {

    public static final String ID = "Context.RunFileOperation";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name="isolate", required = false, values = "false")
    protected boolean isolate = false;


    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        Map<String, Object> vars = isolate ? new HashMap<String, Object>(ctx.getVars()) : ctx.getVars();
        OperationContext subctx = new OperationContext(ctx.getCoreSession(), vars);
        subctx.setInput(blob);
        return (Blob) service.run(subctx, chainId);
    }

    @OperationMethod
    public BlobList run(BlobList blobs) throws Exception {
        BlobList result = new BlobList(blobs.size());
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        return result;
    }

}
