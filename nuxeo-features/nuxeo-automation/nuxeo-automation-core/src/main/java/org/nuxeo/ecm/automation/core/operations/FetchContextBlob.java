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
package org.nuxeo.ecm.automation.core.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = FetchContextBlob.ID, category = Constants.CAT_FETCH, label = "Context File(s)", description = "Fetch the input of the context as a file or list of files. The file(s) will become the input for the next operation.")
public class FetchContextBlob {

    public static final String ID = "Context.FetchFile";

    @Context
    protected OperationContext ctx;

    @OperationMethod(collector=BlobCollector.class)
    public Blob run(Blob blob) throws Exception {
        return blob;
    }

    public BlobList run(BlobList blobs) throws Exception {
        return blobs;
    }

}
