/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.jms;

import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to configured is Async Post Commit EventListener must be process
 * - by the core directly
 * or
 * - by JMS bus
 * <p>
 * (mainly used for testing)
 *
 * @author tiry
 */
public class AsyncProcessorConfig {

    protected static Boolean forceJMSUsage;

    protected static String forceJMSUsageKey = "org.nuxeo.ecm.event.forceJMS";

    public static boolean forceJMSUsage() {
        if (forceJMSUsage == null) {
            String forceFlag = Framework.getProperty(forceJMSUsageKey, "false");
            forceJMSUsage = Boolean.parseBoolean(forceFlag);
        }
        return forceJMSUsage;
    }

    public static void setForceJMSUsage(boolean flag) {
        forceJMSUsage = flag;
    }

}
