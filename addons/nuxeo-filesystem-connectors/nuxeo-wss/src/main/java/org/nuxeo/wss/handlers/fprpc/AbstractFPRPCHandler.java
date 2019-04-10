/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.handlers.fprpc;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fprpc.FPRPCCall;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.FPRPCResponse;
import org.nuxeo.wss.spi.Backend;
import org.nuxeo.wss.spi.WSSBackend;

public abstract class AbstractFPRPCHandler implements FPRPCHandler {

    private static final Log log = LogFactory.getLog(AbstractFPRPCHandler.class);

    public void handleRequest(FPRPCRequest request, FPRPCResponse fpResponse) throws WSSException {
        List<FPRPCCall> calls = request.getCalls();

        if (calls.size() > 1) {
            log.error("multiple calls not implemented, stopping processing");
            throw new WSSException("multiple calls not implemented ");
        }

        WSSBackend backend = Backend.get(request);
        processCall(request, fpResponse, 0, backend);
    }

    protected abstract void processCall(FPRPCRequest request, FPRPCResponse fpResponse, int callIndex,
            WSSBackend backend) throws WSSException;

}
