/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test;

import javax.inject.Inject;

import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 0.1
 */
public class TestBase {

    @Inject
    public CoreSession session;

    public NuxeoClient nuxeoClient;

    public String baseURL;

    public void login() {
        String url = "http://localhost:18090";
        // String url = "http://localhost:8080/nuxeo";
        this.baseURL = url;
        nuxeoClient = new NuxeoClient(url, "Administrator", "Administrator").timeout(60).schemas("*");
    }

    public void login(String username, String pwd) {
        nuxeoClient = new NuxeoClient("http://localhost:18090", username, pwd);
    }

    public void logout() {
        nuxeoClient.logout();
    }

    protected void fetchInvalidations() {
        session.save();
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }
}
