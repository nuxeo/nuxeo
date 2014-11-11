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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Dummy implementation of the {@link ScanService} interface.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class DummyVirusScanner implements ScanService {

    protected static List<String> doneFiles = new ArrayList<String>();

    @Override
    public ScanResult scanBlob(Blob blob) throws ClientException {
        if (blob!=null) {
            doneFiles.add(blob.getFilename());
            if (blob.getFilename().contains("doFail")) {
                throw new ClientException("Virus Scanner not available");
            }
            return new ScanResult(false, "No virus found in " + blob.getFilename());
        } else {
            return new ScanResult(false, "No file found");
        }
    }

    public static List<String> getProcessedFiles() {
        return doneFiles;
    }
}
