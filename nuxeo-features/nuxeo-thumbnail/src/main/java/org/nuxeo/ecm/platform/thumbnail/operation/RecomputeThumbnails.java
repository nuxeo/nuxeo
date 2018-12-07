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
package org.nuxeo.ecm.platform.thumbnail.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.thumbnail.work.ThumbnailRecomputeWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Recomputes the thumbnail of the documents resulting from the provided NXQL query.
 *
 * @since 10.10
 */
@Operation(id = RecomputeThumbnails.ID, category = Constants.CAT_SERVICES, label = "Recompute Thumbnails", description = "Recompute the thumbnail of the documents resulting from the provided NXQL query.", since = "10.10")
public class RecomputeThumbnails {

    public static final String ID = "RecomputeThumbnails";

    public static final String DEFAULT_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = 'Thumbnail' AND thumb:thumbnail/data IS NULL AND ecm:isVersion = 0 AND ecm:isProxy = 0 AND ecm:isTrashed = 0";

    @Context
    protected CoreSession session;

    @Param(name = "query", description = "NXQL query to collect the documents whose thumnail to recompute.", values = {
            DEFAULT_QUERY })
    protected String query;

    @OperationMethod
    public void run() {
        ThumbnailRecomputeWork work = new ThumbnailRecomputeWork(session.getRepositoryName(), query);
        Framework.getService(WorkManager.class).schedule(work);
    }

}
