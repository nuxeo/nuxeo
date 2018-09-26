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
package org.nuxeo.ecm.core.bulk.actions;

import static org.nuxeo.ecm.core.bulk.BulkProcessor.STATUS_STREAM;

import java.util.Arrays;

import org.nuxeo.ecm.core.bulk.actions.computation.CSVProjection;
import org.nuxeo.ecm.core.bulk.actions.computation.ExposeBlob;
import org.nuxeo.ecm.core.bulk.actions.computation.MakeBlob;
import org.nuxeo.ecm.core.bulk.actions.computation.SortBlob;
import org.nuxeo.ecm.core.bulk.actions.computation.ZipBlob;
import org.nuxeo.lib.stream.computation.Topology.Builder;

/**
 * @since 10.3
 */
public class CSVExportAction extends AbstractBulkAction {

    public static final String ACTION_NAME = "csvExport";

    @Override
    protected Builder addComputations(Builder builder, int size, int threshold) {
        return builder.addComputation(() -> new CSVProjection(size, threshold),
                Arrays.asList("i1:" + ACTION_NAME, "o1:" + MakeBlob.NAME))
                      .addComputation(MakeBlob::new, Arrays.asList("i1:" + MakeBlob.NAME, "o1:" + SortBlob.NAME))
                      .addComputation(SortBlob::new, Arrays.asList("i1:" + SortBlob.NAME, "o1:" + ZipBlob.NAME))
                      .addComputation(ZipBlob::new, Arrays.asList("i1:" + ZipBlob.NAME, "o1:" + ExposeBlob.NAME))
                      .addComputation(ExposeBlob::new, Arrays.asList("i1:" + ExposeBlob.NAME, "o1:" + STATUS_STREAM));
    }

}
