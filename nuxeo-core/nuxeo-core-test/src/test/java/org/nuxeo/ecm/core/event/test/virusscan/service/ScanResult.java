/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.event.test.virusscan.service;

/**
 * Encapsulate result from a virus scan on a Blob
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
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
