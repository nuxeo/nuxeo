/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk.action.computation;

import static org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter.TEXT_CSV_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.csv.DocumentModelCSVWriter.SCHEMAS_CTX_DATA;
import static org.nuxeo.ecm.core.io.marshallers.csv.DocumentModelCSVWriter.XPATHS_CTX_DATA;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.action.CSVExportAction;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

/**
 * </ul>
 * Outputs
 * <ul>
 * <li>o1: Writes {@link org.nuxeo.lib.stream.computation.Record} containing csv lines</li>
 * </ul>
 *
 * @since 10.3
 */
public class CSVProjection extends AbstractBulkComputation {

    public static final String SCHEMAS_PARAM = "schemas";

    public static final String XPATHS_PARAM = "xpaths";

    protected ByteArrayOutputStream out;

    public CSVProjection() {
        super(CSVExportAction.ACTION_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        out = new ByteArrayOutputStream();
        DocumentRef[] refs = ids.stream().map(IdRef::new).toArray(DocumentRef[]::new);

        List<String> schemas = properties.get(SCHEMAS_PARAM) != null ? (List<String>) properties.get(SCHEMAS_PARAM)
                : new ArrayList<>();
        List<String> xpaths = properties.get(XPATHS_PARAM) != null ? (List<String>) properties.get(XPATHS_PARAM)
                : new ArrayList<>();

        DocumentModelList docs = session.getDocuments(refs);

        MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
        RenderingContext renderingCtx = RenderingContext.CtxBuilder.get();
        renderingCtx.setParameterValues(SCHEMAS_CTX_DATA, schemas);
        renderingCtx.setParameterValues(XPATHS_CTX_DATA, xpaths);
        Writer<DocumentModelList> writer = registry.getWriter(renderingCtx, DocumentModelList.class, TEXT_CSV_TYPE);
        try {
            writer.write(docs, DocumentModelList.class, null, TEXT_CSV_TYPE, out);
        } catch (IOException e) {
            getLog().error(e, e);
        }
    }

    @Override
    public void endBucket(ComputationContext context, int bucketSize) {
        String commandId = getCurrentCommand().getId();
        DataBucket dataBucket = new DataBucket(commandId, bucketSize, out.toByteArray());
        Record record = Record.of(commandId, BulkCodecs.getDataBucketCodec().encode(dataBucket));
        context.produceRecord(OUTPUT_1, record);
        out = null;
    }

}
