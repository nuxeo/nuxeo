/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
@Operation(id = CreateProxyLive.ID, category = Constants.CAT_DOCUMENT, label = "Create Proxy Live", description = "This operation will create a proxy that points the given document as input. This is like a symbolic link for File System. The proxy will be created into the destination specified as parameter. <p>The document returned is the proxy live.<p> Remark: <b>you will have a strange behavior if the input is a folderish.</b>", aliases = { "CreateProxyLive" })
public class CreateProxyLive {

    public static final String ID = "Document.CreateLiveProxy";

    @Context
    protected CoreSession session;

    @Param(name = "Destination Path")
    protected String path;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) {
        DocumentRef docRef = new PathRef(path);
        if (!session.exists(docRef)) {
            throw new NuxeoException(String.format("Destination \"%s\" specified into operation not found", path));
        }

        DocumentModel proxy = session.createProxy(input.getRef(), docRef);

        return proxy;
    }

}
