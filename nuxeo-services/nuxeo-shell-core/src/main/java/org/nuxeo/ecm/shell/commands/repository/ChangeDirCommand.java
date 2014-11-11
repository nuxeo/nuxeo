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

package org.nuxeo.ecm.shell.commands.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ChangeDirCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(ChangeDirCommand.class);

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        if (elements.length != 1) {
            log.error(cmdLine.getCommand()
                    + " takes exactly one parameter: the path of the directory to go into");
        }

        Path path = new Path(elements[0]);
        DocumentModel doc;
        try {
            doc = context.fetchDocument(path);
        } catch (Exception e) {
            log.error("Failed to retrieve the given folder",e);
            return;
        }
        if (doc.hasFacet(FacetNames.FOLDERISH)) {
            context.setCurrentDocument(doc);
        } else {
            log.error("Target document is not a folder but a "
                    + doc.getType());
        }
    }

}
