/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.trace;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * Automation tracer recording all automation execution traces when mode
 * activated.
 *
 * @since 5.7.3
 */
public class Tracer extends BasedTracer {

    protected Tracer(TracerFactory factory, Boolean printable) {
        super(factory, printable);
    }

    @Override
    public void onOperation(OperationContext context, OperationType type,
            InvokableMethod method, Map<String, Object> params) {
        if (type instanceof ChainTypeImpl) {
            pushContext(type);
            return;
        }
        Call call = new Call(chain, context, type, method, params);
        calls.add(call);
    }

}
