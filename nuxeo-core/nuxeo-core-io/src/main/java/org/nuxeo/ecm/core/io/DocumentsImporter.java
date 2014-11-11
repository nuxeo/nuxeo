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

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.io.exceptions.ImportDocumentException;

/**
 * Simple interface useful to wrap a sequence of calls for performing an import.
 * This could be handy to quickly define an importer and sent it as parameter so
 * the method will be callback.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public interface DocumentsImporter {

    DocumentTranslationMap importDocs(InputStream sourceInputStream)
            throws ImportDocumentException, ClientException, IOException;
}
