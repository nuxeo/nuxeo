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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Dummy implementation of the {@link ScanService} interface.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class DummyVirusScanner implements ScanService {

    protected static List<String> doneFiles = new ArrayList<>();

    @Override
    public ScanResult scanBlob(Blob blob) {
        if (blob != null) {
            doneFiles.add(blob.getFilename());
            if (blob.getFilename().contains("doFail")) {
                throw new NuxeoException("Virus Scanner not available");
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
