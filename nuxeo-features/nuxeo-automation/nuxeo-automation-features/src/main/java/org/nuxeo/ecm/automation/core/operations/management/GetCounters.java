/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.automation.core.operations.management;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.counters.CounterHistoryStack;
import org.nuxeo.runtime.management.counters.CounterManager;

/**
 * Return the data collected by one or more Counters For each counter 3 series are returned , bare values, delta and
 * speed
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = GetCounters.ID, category = Constants.CAT_SERVICES, label = "Retrieve counters values", description = "Retrieve data collected by one or more Counters", addToStudio = false)
public class GetCounters {

    public static final String ID = "Counters.GET";

    @Context
    protected OperationContext ctx;

    @Param(name = "counterNames", required = true)
    protected StringList counterNames;

    @OperationMethod
    public Blob run() throws IOException {

        CounterManager cm = Framework.getService(CounterManager.class);

        Map<String, Object> collection = new LinkedHashMap<>();

        NuxeoPrincipal nuxeoUser = ctx.getPrincipal();
        // Only Administrators can access the counters
        if (nuxeoUser.isAdministrator()) {
            for (String counterName : counterNames) {
                CounterHistoryStack stack = cm.getCounterHistory(counterName);

                // copy and reverse the list
                List<long[]> valueList = new ArrayList<>(stack.getAsList());
                Collections.reverse(valueList);

                // bare values [ [t0,v0], [t1,v1] ...]
                List<List<Number>> valueSerie = new ArrayList<>();
                // delta values [ [t1,v1-v0], [t2,v2-v3] ...]
                List<List<Number>> deltaSerie = new ArrayList<>();
                // speed values [ [t1,v1-v0/t1-t0], ...]
                List<List<Number>> speedSerie = new ArrayList<>();

                float lastTS = 0;
                float lastValue = 0;
                long now = System.currentTimeMillis();
                for (long[] values : valueList) {

                    // use seconds
                    long ts = values[0];
                    float t = (now - ts) / 1000;
                    float value = values[1];
                    Float tFloat = Float.valueOf(ts);

                    // bare values
                    Float bareValue = Float.valueOf(value);
                    valueSerie.add(Arrays.asList(tFloat, bareValue));

                    // delta values
                    Float deltaValue = Float.valueOf(value - lastValue);
                    deltaSerie.add(Arrays.asList(tFloat, deltaValue));

                    if (lastTS > 0) {
                        // speed values
                        float tdelta = lastTS - t;
                        if (tdelta == 0) {
                            tdelta = 1;
                        }
                        Float speedValue = Float.valueOf(60 * (value - lastValue) / (tdelta));
                        speedSerie.add(Arrays.asList(tFloat, speedValue));
                    }
                    lastTS = t;
                    lastValue = value;
                }

                Map<String, Object> counter = new LinkedHashMap<>();
                counter.put("values", valueSerie);
                counter.put("deltas", deltaSerie);
                counter.put("speed", speedSerie);

                collection.put(counterName, counter);
            }
        }

        return Blobs.createJSONBlobFromValue(collection);
    }

}
