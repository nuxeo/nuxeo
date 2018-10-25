/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.picture.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.picture.recompute.ImagingRecomputeWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Recomputes the picture views of the documents resulting from the provided NXQL query.
 *
 * @since 10.3
 */
@Operation(id = RecomputePictureViews.ID, category = Constants.CAT_SERVICES, label = "Recompute Picture Wiews", description = "Recompute the picture views of the documents resulting from the provided NXQL query.", since = "10.3")
public class RecomputePictureViews {

    public static final String ID = "Picture.RecomputeViews";

    public static final String DEFAULT_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = 'Picture' AND picture:views/*/title IS NULL";

    @Context
    protected CoreSession session;

    @Param(name = "query", description = "NXQL query to collect the documents whose picture views to recompute.", values = {
            DEFAULT_QUERY })
    protected String query;

    @OperationMethod
    public void run() {
        ImagingRecomputeWork work = new ImagingRecomputeWork(session.getRepositoryName(), query);
        Framework.getService(WorkManager.class).schedule(work);
    }

}
