/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.management.api;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class ProbeStatus {

    protected boolean neverExecuted = false;

    protected final boolean success;

    protected final Map<String, String> infos = new HashMap<String, String>();

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
        Map<String, String> infos = new HashMap<String, String>();
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

}
