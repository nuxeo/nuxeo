/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.event.test.virusscan.service;

/**
 * Encapsulate result from a virus scan on a Blob
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ScanResult {

    protected final boolean virusDetected;

    protected final String scanInfo;

    protected final boolean error;

    public ScanResult(boolean virusDetected, String scanInfo) {
        this.virusDetected = virusDetected;
        this.scanInfo = scanInfo;
        this.error = false;
    }

    private ScanResult(String scanInfo) {
        this.virusDetected = false;
        this.scanInfo = scanInfo;
        this.error = true;
    }

    public static ScanResult makeFailed(String message) {
        return new ScanResult(message);
    }

    public boolean isVirusDetected() {
        return virusDetected;
    }

    public String getScanInfo() {
        return scanInfo;
    }

    public boolean isError() {
        return error;
    }

}
