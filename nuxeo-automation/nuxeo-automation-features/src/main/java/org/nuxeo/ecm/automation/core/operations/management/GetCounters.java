/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.automation.core.operations.management;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.counters.CounterHistoryStack;
import org.nuxeo.runtime.management.counters.CounterManager;

/**
* Return the data collected by one or more Counters
*
* For each counter 3 series are returned , bare values, delta and speed
*
* @author Tiry (tdelprat@nuxeo.com)
*
*/
@Operation(id = GetCounters.ID, category = Constants.CAT_SERVICES, label = "Retrieve counters values", description = "Retrieve data collected by one or more Counters")
public class GetCounters {

    public static final String ID = "Counters.GET";

    @Context
    protected OperationContext ctx;

    @Param(name = "counterNames", required = true)
    protected StringList counterNames;

    @OperationMethod
    public Blob run() throws Exception {

        CounterManager cm = Framework.getLocalService(CounterManager.class);

        JSONObject collection = new JSONObject();

        Principal principal = ctx.getPrincipal();
        if (principal instanceof NuxeoPrincipal) {
            NuxeoPrincipal nuxeoUser = (NuxeoPrincipal) principal;
            // Only Administrators can access the counters
            if (nuxeoUser.isAdministrator()) {
                for (String counterName : counterNames) {
                    CounterHistoryStack stack = cm.getCounterHistory(counterName);

                    // copy and reverse the list
                    List<long[]> valueList = new ArrayList<long[]>(stack.getAsList());
                    Collections.reverse(valueList);

                    JSONObject counter = new JSONObject();

                    // bare values [ [t0,v0], [t1,v1] ...]
                    JSONArray valueSerie = new JSONArray();
                    // delta values [ [t1,v1-v0], [t2,v2-v3] ...]
                    JSONArray deltaSerie = new JSONArray();
                    // speed values [ [t1,v1-v0/t1-t0], ...]
                    JSONArray speedSerie = new JSONArray();

                    float lastTS = 0;
                    float lastValue = 0;
                    long now = System.currentTimeMillis();
                    for (long[] values : valueList) {

                        // use seconds
                        long ts =values[0];
                        float t = (now - ts)/1000;
                        float value = values[1];

                        JSONArray valueArray = new JSONArray();
                        JSONArray deltaArray = new JSONArray();
                        JSONArray speedArray = new JSONArray();

                        // bare values
                        valueArray.add(ts);
                        valueArray.add(value);
                        valueSerie.add(valueArray);

                        // delta values
                        deltaArray.add(ts);
                        deltaArray.add(value - lastValue);
                        deltaSerie.add(deltaArray);

                        if (lastTS>0) {
                            // speed values
                            speedArray.add(ts);
                            float tdelta = lastTS-t;
                            if (tdelta==0) {
                                tdelta=1;
                            }
                            speedArray.add(60*(value - lastValue) / (tdelta));
                            speedSerie.add(speedArray);
                        }
                        lastTS = t;
                        lastValue = value;
                    }

                    counter.put("values", valueSerie);
                    counter.put("deltas", deltaSerie);
                    counter.put("speed", speedSerie);

                    collection.put(counterName, counter);
                }
            }
        }


        return new ByteArrayBlob(collection.toString().getBytes("UTF-8"),
        "application/json");
    }

}
