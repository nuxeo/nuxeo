/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.csv.export.computation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter.TEXT_CSV_TYPE;
import static org.nuxeo.ecm.platform.csv.export.io.DocumentModelCSVWriter.SCHEMAS_CTX_DATA;
import static org.nuxeo.ecm.platform.csv.export.io.DocumentModelCSVWriter.XPATHS_CTX_DATA;
import static org.nuxeo.ecm.platform.csv.export.io.DocumentPropertyCSVWriter.LANG_CTX_DATA;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.ecm.core.io.marshallers.csv.OutputStreamWithCSVWriter;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.csv.export.action.CSVExportAction;
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
public class CSVProjectionComputation extends AbstractBulkComputation {

    private static final Logger log = LogManager.getLogger(CSVProjectionComputation.class);

    public static final String PARAM_SCHEMAS = "schemas";

    public static final String PARAM_XPATHS = "xpaths";

    public static final String PARAM_LANG = "lang";

    protected OutputStreamWithCSVWriter out;

    protected RenderingContext renderingCtx;

    public CSVProjectionComputation() {
        super(CSVExportAction.ACTION_FULL_NAME);
    }

    @Override
    public void startBucket(String bucketKey) {
        out = new OutputStreamWithCSVWriter();
        BulkCommand command = getCurrentCommand();
        renderingCtx = RenderingContext.CtxBuilder.get();
        renderingCtx.setParameterValues(SCHEMAS_CTX_DATA, getList(command.getParams().get(PARAM_SCHEMAS)));
        renderingCtx.setParameterValues(XPATHS_CTX_DATA, getList(command.getParams().get(PARAM_XPATHS)));
        renderingCtx.setParameterValues(LANG_CTX_DATA, getString(command.getParams().get(PARAM_LANG)));
    }

    @Override
    protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        DocumentModelList docs = loadDocuments(session, ids);
        MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
        Writer<DocumentModelList> writer = registry.getWriter(renderingCtx, DocumentModelList.class, TEXT_CSV_TYPE);
        try {
            writer.write(docs, DocumentModelList.class, null, TEXT_CSV_TYPE, out);
        } catch (IOException e) {
            log.error("Unable to write documents", e);
        }
    }

    @Override
    public void endBucket(ComputationContext context, BulkStatus delta) {
        String commandId = delta.getId();
        // Extract header from data
        String csv = out.toString();
        String recordSeparator = CSVFormat.DEFAULT.getRecordSeparator();
        String header = getHeader(csv, recordSeparator);
        String data = getData(csv, recordSeparator);
        DataBucket dataBucket = new DataBucket(commandId, delta.getProcessed(), data.getBytes(UTF_8),
                header.getBytes(UTF_8), new byte[0]);
        Record record = Record.of(commandId, BulkCodecs.getDataBucketCodec().encode(dataBucket));
        context.produceRecord(OUTPUT_1, record);
        out = null;
    }

    protected String getHeader(String csv, String recordSeparator) {
        return csv.substring(0, csv.indexOf(recordSeparator) + recordSeparator.length());
    }

    protected String getData(String csv, String recordSeparator) {
        return csv.substring(csv.indexOf(recordSeparator) + recordSeparator.length());
    }

    protected List<String> getList(Serializable value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?>) {
            List<?> objects = (List<?>) value;
            List<String> values = new ArrayList<>(objects.size());
            for (Object object : objects) {
                if (object != null) {
                    values.add(object.toString());
                }
            }
            Collections.sort(values);
            return values;
        } else {
            log.debug("Illegal parameter '{}'", value);
            return Collections.emptyList();
        }
    }

    protected String getString(Serializable value) {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

}
