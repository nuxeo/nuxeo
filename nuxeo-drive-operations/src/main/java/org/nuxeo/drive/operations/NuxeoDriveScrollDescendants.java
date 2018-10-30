/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.io.IOException;
import java.io.StringWriter;

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Retrieves at most {@code batchSize} descendants of the {@link FolderItem} with the given {@code id} for the currently
 * authenticated user and the given {@code scrollId}.
 * <p>
 * When passing a null {@code scrollId} the initial search request is executed and the first batch of results is
 * returned along with a {@code scrollId} which should be passed to the next call in order to retrieve the next batch of
 * results.
 * <p>
 * Ideally, the search context made available by the initial search request is kept alive during {@code keepAlive}
 * milliseconds if {@code keepAlive} is positive.
 * <p>
 * Results are not necessarily sorted.
 *
 * @since 8.3
 */
@Operation(id = NuxeoDriveScrollDescendants.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Scroll descendants")
public class NuxeoDriveScrollDescendants {

    public static final String ID = "NuxeoDrive.ScrollDescendants";

    @Context
    protected OperationContext ctx;

    @Param(name = "id")
    protected String id;

    @Param(name = "scrollId", required = false)
    protected String scrollId;

    @Param(name = "batchSize")
    protected int batchSize;

    @Param(name = "keepAlive", required = false)
    protected long keepAlive = 60000; // 1 minute

    @OperationMethod
    public Blob run() throws IOException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        ScrollFileSystemItemList descendants = fileSystemItemManager.scrollDescendants(id, ctx.getPrincipal(), scrollId,
                batchSize, keepAlive);
        return writeJSONBlob(descendants);
    }

    protected Blob writeJSONBlob(ScrollFileSystemItemList scrollFSIList) throws IOException {
        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator jg = factory.createGenerator(writer)) {
            jg.setCodec(new ObjectMapper());
            jg.writeStartObject();
            jg.writeStringField("scrollId", scrollFSIList.getScrollId());
            jg.writeObjectField("fileSystemItems", scrollFSIList);
            jg.writeEndObject();
        }
        return Blobs.createJSONBlob(writer.toString());
    }

}
