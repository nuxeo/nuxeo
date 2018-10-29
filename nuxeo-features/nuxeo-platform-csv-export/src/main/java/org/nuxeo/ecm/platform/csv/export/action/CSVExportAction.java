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
package org.nuxeo.ecm.platform.csv.export.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.PRODUCE_IMMEDIATE_OPTION;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_2;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_3;

import java.util.Arrays;
import java.util.Map;

import org.nuxeo.ecm.core.bulk.action.computation.ExposeBlob;
import org.nuxeo.ecm.core.bulk.action.computation.MakeBlob;
import org.nuxeo.ecm.core.bulk.action.computation.SortBlob;
import org.nuxeo.ecm.core.bulk.action.computation.ZipBlob;
import org.nuxeo.ecm.platform.csv.export.computation.CSVProjection;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 10.3
 */
public class CSVExportAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "csvExport";

    @Override
    public Topology getTopology(Map<String, String> options) {
        boolean produceImmediate = getOptionAsBoolean(options, PRODUCE_IMMEDIATE_OPTION, false);
        return Topology.builder()
                       .addComputation(CSVProjection::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                                       OUTPUT_1 + ":" + MakeBlob.NAME))
                       .addComputation(() -> new MakeBlob(produceImmediate),
                               Arrays.asList(INPUT_1 + ":" + MakeBlob.NAME, //
                                       OUTPUT_1 + ":" + SortBlob.NAME, //
                                       OUTPUT_2 + ":" + ZipBlob.NAME, //
                                       OUTPUT_3 + ":" + ExposeBlob.NAME))
                       .addComputation(SortBlob::new, Arrays.asList(INPUT_1 + ":" + SortBlob.NAME, //
                               OUTPUT_1 + ":" + ZipBlob.NAME, //
                               OUTPUT_2 + ":" + ExposeBlob.NAME))
                       .addComputation(ZipBlob::new, Arrays.asList(INPUT_1 + ":" + ZipBlob.NAME, //
                               OUTPUT_1 + ":" + ExposeBlob.NAME))
                       .addComputation(ExposeBlob::new, Arrays.asList(INPUT_1 + ":" + ExposeBlob.NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static boolean getOptionAsBoolean(Map<String, String> options, String option, boolean defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Boolean.valueOf(value);
    }

}
