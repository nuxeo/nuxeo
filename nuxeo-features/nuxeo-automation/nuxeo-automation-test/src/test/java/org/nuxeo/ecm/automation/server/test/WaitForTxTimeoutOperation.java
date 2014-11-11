/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.server.test;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.platform.web.common.ServletHelper;

@Operation(id = WaitForTxTimeoutOperation.ID, category = Constants.CAT_EXECUTION, label = "TxTimeout", description = "Wait for tx timeout")
public class WaitForTxTimeoutOperation {

    public static final String ID = "WaitForTxTimeout";


    @Context protected OperationContext context;

    @OperationMethod
    public void run() throws Exception {
        HttpServletRequest req = (HttpServletRequest)context.get("request");
        String delay = req.getHeader(ServletHelper.TX_TIMEOUT_HEADER_KEY);
        if (delay == null) {
            return;
        }
        long value = Integer.parseInt(delay) * 1000L * 2;
        Thread.sleep(value);
    }
}
