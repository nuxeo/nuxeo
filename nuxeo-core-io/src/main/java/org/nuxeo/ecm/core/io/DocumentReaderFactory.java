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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.io;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Interface for a factory that will provide a custom DocumentReader implementation.
 * The params are used by specific factories to properly instantiate the custom
 * DocumentReader.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface DocumentReaderFactory {

    DocumentReader createDocReader(Map<String, Object> params) throws ClientException;

}
