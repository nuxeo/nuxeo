/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.core.management.statuses;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.management.api.ProbeInfo;

/**
 * Returns the status of the application
 * 
 * @since 9.3
 */
public class HealthCheckResult {

    public final Log log = LogFactory.getLog(HealthCheckResult.class);

    protected Collection<ProbeInfo> probes;

    protected boolean healthy = true;

    public HealthCheckResult(Collection<ProbeInfo> probesToCheck) {
        this.probes = probesToCheck;
        for (ProbeInfo probeInfo : probesToCheck) {
            healthy = healthy && probeInfo.getStatus().isSuccess();
        }
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String toJson() {
        try {
            return buildResponse(probes).getString();
        } catch (IOException e) {
            log.error("Unable to compute detailed healthCheck status", e);
            return StringUtils.EMPTY; // don't fail as the probes might have been sucessful
        }
    }

    protected Blob buildResponse(Collection<ProbeInfo> probes) {
        JSONArray array = new JSONArray();
        try {
            for (ProbeInfo probe : probes) {
                JSONObject o = new JSONObject();
                o.put(probe.getShortcutName(), (probe.getStatus().isSuccess() ? "ok" : "failed"));
                array.put(o);
            }
            JSONObject result = new JSONObject();
            result.put("healthCheck", array);
            return Blobs.createBlob(result.toString(), "application/json");
        } catch (JSONException e) {
            log.error("Unable to compute detailed healthCheck status", e);
        }
        return new StringBlob(StringUtils.EMPTY);
    }
}
