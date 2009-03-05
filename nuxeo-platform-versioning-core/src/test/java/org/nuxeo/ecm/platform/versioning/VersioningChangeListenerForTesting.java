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
package org.nuxeo.ecm.platform.versioning;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.versioning.listeners.VersioningChangeEventListener;


/**
 * Listener for testing purposes only. It checks the versions (major and minor)
 * of the received old and new document models against the statically set numbers.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class VersioningChangeListenerForTesting extends VersioningChangeEventListener {

    private static final Log log = LogFactory.getLog(VersioningChangeListenerForTesting.class);

    public static VersioningChangeListenerForTesting instance;

    // these are the values that should be checked against when event is
    // received
    private static long oldDoc_majV;
    private static long oldDoc_minV;
    private static long newDoc_majV;
    private static long newDoc_minV;

    protected DocumentModel newDoc;

    protected DocumentModel oldDoc;

    public VersioningChangeListenerForTesting() {
        log.info("<init>");
    }

    @Override
    protected void versionsChangeNotify(DocumentModel newDoc,
            DocumentModel oldDoc) {

        try {
            VersioningDocument vOldDoc = oldDoc.getAdapter(VersioningDocument.class);
            if (null == vOldDoc) {
                throw new IllegalStateException("VersioningDocument adapter not available.");
            }
            log.info("oldDoc version: " + vOldDoc.getVersionAsString(2, 2, '.'));
            VersioningDocument vNewDoc = newDoc.getAdapter(VersioningDocument.class);
            log.info("newDoc version: " + vNewDoc.getVersionAsString(2, 2, '.'));

            TestCase.assertEquals(oldDoc_majV,
                    vOldDoc.getMajorVersion().longValue());
            TestCase.assertEquals(oldDoc_minV,
                    vOldDoc.getMinorVersion().longValue());
            TestCase.assertEquals(newDoc_majV,
                    vNewDoc.getMajorVersion().longValue());
            TestCase.assertEquals(newDoc_minV,
                    vNewDoc.getMinorVersion().longValue());

        } catch (DocumentException e) {
            log.error(e.getMessage(), e);
        }

        instance = this;
        this.newDoc = newDoc;
        this.oldDoc = oldDoc;
    }

    public static void setVersionsToCheck(long old_maj, long old_min,
            long new_maj, long new_min) {
        oldDoc_majV = old_maj;
        oldDoc_minV = old_min;
        newDoc_majV = new_maj;
        newDoc_minV = new_min;
    }

}
