/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.automation.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Returns tasks assigned to current user or one of its groups.
 *
 * @since 5.5
 */
@Operation(id = WaitForIndexing.ID, category = Constants.CAT_SERVICES, label = "Wait for Elasticsearch Indexing",
        since = "8.1",
        description = "Wait until Elasticsearch indexing is done.")
public class WaitForIndexing {

    public static final String ID = "Elasticsearch.WaitForIndexing";

    private static final Log log = LogFactory.getLog(Log.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected ElasticSearchAdmin esa;

    @Context
    protected CoreSession repo;

    @Param(name = "timeoutSecond", required = false)
    protected Integer timeout = 60;

    @Param(name = "refresh", required = false)
    protected Boolean refresh = false;

    @OperationMethod
    public Boolean run() {
        try {
            esa.prepareWaitForIndexing().get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return Boolean.FALSE;
        } catch (InterruptedException e) {
            return Boolean.FALSE;
        } catch (ExecutionException e) {
            return Boolean.FALSE;
        }
        if (refresh) {
            esa.refreshRepositoryIndex(repo.getRepositoryName());
        }
        return Boolean.TRUE;
    }


}
