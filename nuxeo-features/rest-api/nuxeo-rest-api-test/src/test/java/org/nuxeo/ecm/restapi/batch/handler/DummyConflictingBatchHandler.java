/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Luís Duarte <lduarte@nuxeo.com>
 *     Florent Guillaume <fguillaume@nuxeo.com>
 *
 */
package org.nuxeo.ecm.restapi.batch.handler;

import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.impl.DefaultBatchHandler;

public class DummyConflictingBatchHandler extends DefaultBatchHandler {

    @Override
    public boolean completeUpload(String batchId, String fileIdx, BatchFileInfo fileInfo) {
        return false;
    }

}
