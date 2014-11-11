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
 */

package org.nuxeo.ecm.automation.core.trace;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * Automation tracer recording lightweight automation execution traces when mode
 * deactivated.
 *
 * @since 5.7.3
 */
public class TracerLite extends Tracer {

    protected TracerLite(TracerFactory factory) {
        super(factory, true);
    }

    @Override
    public void onOperation(OperationContext context, OperationType type,
            InvokableMethod method, Map<String, Object> parms) {
        if (type instanceof ChainTypeImpl) {
            pushContext(type);
            return;
        }
        Call call = new Call(chain, null, type, null, null);
        calls.add(call);
    }

}
