/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.web.sync;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.operations.RefreshUI;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;

@Operation(id = RefreshUI.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Refresh", description = "Refresh the UI cache. This is a void operation - the input object is returned back as the oputput", addToStudio = false)
public class RefreshUISync extends RefreshUI {

    @Override
    @OperationMethod
    public void run() {
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        super.run();
    }

}
