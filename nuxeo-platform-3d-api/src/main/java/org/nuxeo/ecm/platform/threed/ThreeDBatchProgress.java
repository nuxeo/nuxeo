/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed;

/**
 * Simple data transfer object to report on the state of a 3D content work or conversion
 *
 * @since 8.4
 */
public class ThreeDBatchProgress {

    public static final String STATUS_CONVERSION_QUEUED = "status.threed.conversionQueued";

    public static final String STATUS_CONVERSION_RUNNING = "status.threed.conversionRunning";

    public static final String STATUS_CONVERSION_UNKNOWN = "status.threed.conversionUnknown";

    public final String status;

    public final String message;

    public ThreeDBatchProgress(String status, String message) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUnknown() {
        return this.status.equals(STATUS_CONVERSION_UNKNOWN);
    }

    public boolean isQueued() {
        return this.status.equals(STATUS_CONVERSION_QUEUED);
    }

    public boolean isRunning() {
        return this.status.equals(STATUS_CONVERSION_RUNNING);
    }

}
