/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.runtime.management.api;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProbeStatus {

    protected boolean neverExecuted = false;

    protected final boolean success;

    protected final Map<String, String> infos = new HashMap<>();

    public static final String DEFAULT_INFO_FIELD = "info";

    public static final String ERROR_FIELD = "error";

    protected ProbeStatus(String info, Boolean success) {
        this.infos.put(DEFAULT_INFO_FIELD, info);
        this.success = success;
    }

    protected ProbeStatus(Map<String, String> infos, Boolean success) {
        this.infos.putAll(infos);
        this.success = success;
    }

    public static ProbeStatus newBlankProbStatus() {
        ProbeStatus status = new ProbeStatus("[unavailable]", false);
        status.neverExecuted = true;
        return status;
    }

    public static ProbeStatus newFailure(String info) {
        return new ProbeStatus(info, FALSE);
    }

    public static ProbeStatus newFailure(Map<String, String> infos) {
        return new ProbeStatus(infos, FALSE);
    }

    public static ProbeStatus newError(Throwable t) {
        Map<String, String> infos = new HashMap<>();
        infos.put(ERROR_FIELD, t.toString());
        infos.put(DEFAULT_INFO_FIELD, "Caught error " + t.toString());
        return new ProbeStatus(infos, FALSE);
    }

    public static ProbeStatus newSuccess(String info) {
        return new ProbeStatus(info, TRUE);
    }

    public static ProbeStatus newSuccess(Map<String, String> infos) {
        return new ProbeStatus(infos, TRUE);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public boolean isNeverExecuted() {
        return neverExecuted;
    }

    public String getAsString() {
        if (infos == null || infos.isEmpty()) {
            return Boolean.toString(success);
        }
        if (infos.size() == 1) {
            return infos.values().iterator().next();
        }

        StringBuilder sb = new StringBuilder();
        for (String key : infos.keySet()) {
            sb.append(key);
            sb.append(" : ");
            sb.append(infos.get(key));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getAsString();
    }

    public String getAsXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<dl>");
        for (String key : infos.keySet()) {
            sb.append("<dt>");
            sb.append(key);
            sb.append("</dt>");
            sb.append("<dd class='").append(key).append("'>");
            sb.append(infos.get(key));
            sb.append("</dd>");
        }
        sb.append("</dl>");
        return sb.toString();
    }

    /**
     * @since 11.1
     */
    public Map<String, String> getInfos() {
        return Collections.unmodifiableMap(infos);
    }

}
