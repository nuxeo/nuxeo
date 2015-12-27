/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.jms;

import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to configured is Async Post Commit EventListener must be processed:
 * <ul>
 * <li>by the core directly, or
 * <li>by the JMS bus.
 * <p>
 * (Mainly used for testing).
 *
 * @author tiry
 */
public class AsyncProcessorConfig {

    protected static Boolean forceJMSUsage;

    protected static final String forceJMSUsageKey = "org.nuxeo.ecm.event.forceJMS";

    public static boolean forceJMSUsage() {
        if (forceJMSUsage == null) {
            forceJMSUsage = Boolean.valueOf(Framework.isBooleanPropertyTrue(forceJMSUsageKey));
        }
        return forceJMSUsage;
    }

    public static void setForceJMSUsage(boolean flag) {
        forceJMSUsage = flag;
    }

}
