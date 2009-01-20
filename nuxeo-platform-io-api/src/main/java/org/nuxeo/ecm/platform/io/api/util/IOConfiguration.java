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

package org.nuxeo.ecm.platform.io.api.util;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.io.api.IOManager;

/**
 * Define a destination for a Core IO operation
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface IOConfiguration {

    String DOC_READER_FACTORY = "org.nuxeo.ecm.core.io.doc_reader_factory";
    String DOC_WRITER_FACTORY = "org.nuxeo.ecm.core.io.doc_writer_factory";

    IOManager getManager();

    String getRepositoryName();

    void setRepositoryName(String repositoryName);

    Collection<DocumentRef> getDocuments();

    DocumentRef getFirstDocument();

    void addDocument(DocumentRef docRef);

    void setProperty(String name, Object serializable);

    Object getProperty(String name);

    Map<String,Object > getProperties();

    boolean isLocal();

}
