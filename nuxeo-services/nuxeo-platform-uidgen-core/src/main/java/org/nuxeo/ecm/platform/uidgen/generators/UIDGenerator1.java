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

package org.nuxeo.ecm.platform.uidgen.generators;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.uidgen.AbstractUIDGenerator;

public class UIDGenerator1 extends AbstractUIDGenerator {

    private static final String P1 = "geide:atelier_emetteur";

    private static final String P2 = "geide:application_emetteur";

    private static final String NFORMAT = "%05d";

    public String getSequenceKey(DocumentModel doc) throws DocumentException {
        Calendar cal = new GregorianCalendar();
        int year = cal.get(Calendar.YEAR);

        // should assume that P2 is always defined

        return (isPropValueDefined(P1, doc) ? str(P1, doc) : str(P2, doc))
                + year;
    }

    public String createUID(DocumentModel doc) throws DocumentException {
        int index = getNext(doc);
        String n = String.format(NFORMAT, index);

        final String seqKey = getSequenceKey(doc);

        return seqKey + n;
    }
}
