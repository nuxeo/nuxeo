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

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(ViewCommand.class);

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        DocumentModel doc;
        if (elements.length == 1) {
            Path path = new Path(elements[0]);
            try {
                doc = context.fetchDocument(path);
            } catch (Exception e) {
                log.error("Failed to retrieve the given folder",e);
                return;
            }
        } else {
            doc = context.fetchDocument();
        }

        Calendar cal = (Calendar)doc.getProperty("dublincore", "created");
        Date ctime = cal == null ? null : cal.getTime();
        cal = (Calendar)doc.getProperty("dublincore", "modified");
        Date mtime = cal == null ? null : cal.getTime();
        log.info("--------------------------------------------------------------------");
        log.info("UID: "+doc.getId());
        log.info("Path: "+doc.getPathAsString());
        log.info("Type: "+doc.getType());
        log.info("--------------------------------------------------------------------");
        log.info("Title: "+doc.getTitle());
        log.info("Author: "+doc.getProperty("dublincore", "creator"));
        log.info("Created: "+ctime);
        log.info("Last Modified: "+mtime);
        log.info("--------------------------------------------------------------------");
        log.info("Description: "+doc.getTitle());
        log.info("--------------------------------------------------------------------");
    }

}
