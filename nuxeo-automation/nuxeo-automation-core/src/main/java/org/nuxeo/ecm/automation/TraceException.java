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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation;

import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
public class TraceException extends OperationException {

    private static final long serialVersionUID = 1L;

    TracerFactory traceFactory = Framework.getLocalService(TracerFactory.class);

    public TraceException(OperationCallback tracer, Throwable cause) {
        super(tracer.getFormattedText(), cause);
    }

    public TraceException(String message) {
        super(message);
    }

    public TraceException(Throwable cause) {
        super(cause);
    }

}
