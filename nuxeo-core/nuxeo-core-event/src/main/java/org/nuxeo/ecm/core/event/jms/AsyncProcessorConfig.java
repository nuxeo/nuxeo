/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * <li> by the core directly, or
 * <li> by the JMS bus.
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
            String forceFlag = Framework.getProperty(forceJMSUsageKey, "false");
            forceJMSUsage = Boolean.parseBoolean(forceFlag);
        }
        return forceJMSUsage;
    }

    public static void setForceJMSUsage(boolean flag) {
        forceJMSUsage = flag;
    }

}
