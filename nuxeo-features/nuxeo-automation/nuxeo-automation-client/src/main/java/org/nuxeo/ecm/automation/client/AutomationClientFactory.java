/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Stephane Lacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * @since 5.7 Automation Client Service factory
 */
public interface AutomationClientFactory {

    AutomationClient getClient(URL url) throws URISyntaxException;

    AutomationClient getClient(URL url, int httpCxTimeout)
            throws URISyntaxException;
}
