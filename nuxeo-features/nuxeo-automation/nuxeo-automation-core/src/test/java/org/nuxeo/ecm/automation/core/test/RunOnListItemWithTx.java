/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transaction;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Operation(id = "runOnListItemWithTx")
public class RunOnListItemWithTx {

    @Context
    OperationContext ctx;

    @Context
    CoreSession session;

    protected List<String> getOrCreateList(String name) {
        List<String> list = (List<String>) ctx.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            ctx.put(name, list);
        }
        return list;
    }

    @OperationMethod
    public void printInfo() throws Exception {

        // session.query("select * from Document");

        String user = (String) ctx.get("item");
        Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
        getOrCreateList("result").add(user);
        getOrCreateList("sids").add(session.getSessionId());
        getOrCreateList("txids").add(tx.toString());

    }

}
