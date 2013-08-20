/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */

package org.nuxeo.ecm.automation;

import java.util.Map;

import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.trace.Trace;

/**
 * @since 5.7.3
 */
public interface OperationCallback {

    void onChain(OperationType chain);

    void onOperation(OperationContext context, OperationType type,
            InvokableMethod method, Map<String, Object> parms);

    void onError(OperationException error);

    void onOutput(Object output);

    Trace getTrace();

    String getFormattedText();
}
