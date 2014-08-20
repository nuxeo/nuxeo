/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.directory;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.webengine.WebException;

/**
 *
 *
 * @since 5.7.3
 */
public abstract class DirectorySessionRunner<T> {

    abstract T run(Session session) throws ClientException;

    public static <T> T withDirectorySession(Directory directory,
            DirectorySessionRunner<T> runner) {
        Session session = null;
        try {
            session = directory.getSession();
            return runner.run(session);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                    throw WebException.wrap(e);
                }
            }
        }
    }

}
