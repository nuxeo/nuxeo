/*
 *  (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Thierry Casanova
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * Load entries into a {@link org.nuxeo.ecm.directory.Directory} from a CSV File. Depending on
 * {@code dataLoadingPolicy}, duplicate entries could:
 * <ul>
 * <li>be ignored</li>
 * <li>be updated</li>
 * <li>trigger an error</li>
 * </ul>
 *
 * @since 11.1
 */
@Operation(id = LoadFromCSV.ID, category = Constants.CAT_SERVICES, label = "Load directory entries from CSV file", description = "Load directory entries from a CSV file. Depending on the data loading policy, duplicate entries are ignored, updated or trigger an error.")
public class LoadFromCSV extends AbstractDirectoryOperation {

    public static final String ID = "Directory.LoadFromCSV";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Param(name = "directoryName")
    protected String directoryName;

    @Param(name = "dataLoadingPolicy")
    protected String dataLoadingPolicy;

    @OperationMethod
    public void run(Blob dataBlob) {
        validateCanManageDirectories(ctx);
        directoryService.loadFromCSV(directoryName, dataBlob, dataLoadingPolicy);
    }
}
