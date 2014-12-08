/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.model.Session;
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

        Session sqlSession = ((AbstractSession) session).getSession();

        if (sqlSession == null) {
            System.out.println("No SQl Session !!!");
        } else {
            getOrCreateList("sqlsids").add("" + sqlSession.hashCode());
        }
    }

}
