/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.domains;

import java.io.IOException;

import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.runtime.deploy.FileChangeListener;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebDomain extends FileChangeListener {

    WebEngine2 getEngine();

    String getId();

    String getType();

    String getPath();

    // TODO this is specific to a document domain
    String getRoot();

    ScriptFile getIndexPage();

    ScriptFile getErrorPage();

    ScriptFile getDefaultPage();

    String getScriptExtension();

    String getTemplateExtension();

    void flushCache();

    ScriptFile getFile(String path) throws IOException;

}
