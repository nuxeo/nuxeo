/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Context
    protected OperationContext context;

    @OperationMethod
    public String run() throws Exception {
        HttpServletRequest req = (HttpServletRequest) context.get("request");
        String delay = req.getHeader(ServletHelper.TX_TIMEOUT_HEADER_KEY);
        if (delay == null) {
            return "null";
        }
        long value = Integer.parseInt(delay) * 1000L * 2;
        Thread.sleep(value);
        // send a result so that when it's flushed (buffering stops), there's no way to later change the http status
        return delay;
    }
}
