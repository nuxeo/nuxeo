/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public interface Probe {

    ProbeStatus run() throws ClientException;

}
