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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.bulk.action.CSVExportAction;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
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

    protected static final String KEY_SEP = ";";

    protected ByteArrayOutputStream out;

    public CSVProjection(int size) {
        super(CSVExportAction.ACTION_NAME, size);
    }

    @Override
    protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        out = new ByteArrayOutputStream();
        DocumentRef[] refs = ids.stream().map(IdRef::new).toArray(DocumentRef[]::new);
        DocumentModelList list = session.getDocuments(refs);
        MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
        Writer<DocumentModelList> writer = registry.getWriter(null, DocumentModelList.class, TEXT_CSV_TYPE);
        try {
            writer.write(list, DocumentModelList.class, null, TEXT_CSV_TYPE, out);
        } catch (IOException e) {
            getLog().error(e, e);
        }
    }

    @Override
    public void endBucket(ComputationContext context, int bucketSize) {
        Record record = Record.of(buildRecordKey(command.getId(), bucketSize), out.toByteArray());
        context.produceRecord("o1", record);
        out = null;
    }

    public static String buildRecordKey(String commandId, long documentCount) {
        return commandId + KEY_SEP + Long.toString(documentCount);
    }

    public static String getCommandIdFromKey(String key) {
        if (key == null || !key.contains(KEY_SEP)) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key.split(KEY_SEP)[0];
    }

    public static int getDocumentCountFromKey(String key) {
        if (key == null || !key.contains(KEY_SEP)) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return Integer.parseInt(key.split(KEY_SEP)[1]);
    }

}
