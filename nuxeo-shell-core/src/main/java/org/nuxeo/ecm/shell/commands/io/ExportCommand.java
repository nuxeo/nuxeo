/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.shell.commands.io;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDirectoryWriter;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ExportCommand implements Command {

    private static final Log log = LogFactory.getLog(ExportCommand.class);

    private final NuxeoClient client = NuxeoClient.getInstance();

    private RepositoryInstance repository;

    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        // parse cmd line
        if (elements.length != 2) {
            log.error("Usage : export src dest");
            return;
        }
        String path = elements[0];
        File file = new File(elements[1]);

        // open a session
        repository = client.openRepository();
        try {
            // run export
            exportTree(path, file);
        } finally {
            repository.close();
        }
    }

    void exportTree(String fromPath, File file) throws Exception {
        if (fromPath == null || file == null) {
            log.error("Command Syntax Error. See help page");
            return;
        }

        DocumentReader reader = null;
        DocumentWriter writer = null;
        try {
            DocumentModel root = repository.getDocument(new PathRef(fromPath));
            reader = new DocumentTreeReader(repository, root, false);
            // ((DocumentModelReader)reader).setInlineBlobs(true);
            writer = new XMLDirectoryWriter(file);

            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

}
