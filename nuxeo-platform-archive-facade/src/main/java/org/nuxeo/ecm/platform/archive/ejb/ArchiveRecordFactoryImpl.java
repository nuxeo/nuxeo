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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.archive.ejb;

import java.lang.reflect.Constructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecord;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecordFactory;

/**
 * The default archive record factory.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 *
 */
public class ArchiveRecordFactoryImpl implements ArchiveRecordFactory {

    private static final long serialVersionUID = 0L;

    private static final Log log = LogFactory.getLog(ArchiveRecordFactoryImpl.class);

    public ArchiveRecord generateArchiveRecordFrom(Object currentDocument)
            throws Exception {
        @SuppressWarnings("unchecked")
        Class<ArchiveRecord> klass = getArchiveRecordClass();
        ArchiveRecord archiveRecord = null;
        if (klass != null) {
            @SuppressWarnings("unchecked")
            Class[] params = new Class[1];
            params[0] = DocumentModel.class;
            Constructor<ArchiveRecord> constructor = klass.getConstructor(params);
            if (constructor != null) {
                archiveRecord = constructor.newInstance(currentDocument);
            } else {
                log.error("Factory does not have any associated archive record klass !");
            }
        }
        return archiveRecord;
    }

    @SuppressWarnings("unchecked")
    public Class getArchiveRecordClass() {
        return ArchiveRecordImpl.class;
    }
}
