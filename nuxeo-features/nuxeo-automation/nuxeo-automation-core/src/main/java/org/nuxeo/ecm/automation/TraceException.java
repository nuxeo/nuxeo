/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
