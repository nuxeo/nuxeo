/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.uidgen;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.uidgen.AbstractUIDGenerator;

/**
 * Simulates a dummy UID generation based on a property.
 *
 * @since 7.4
 */
public class DummyCoreDocUIDGen extends AbstractUIDGenerator {

    private static final String PROP = "dc:format";

    private static final String FORMAT = "%05d";

    @Override
    public String getSequenceKey(DocumentModel doc) {
        String prefix = (String) doc.getPropertyValue(PROP);
        if (StringUtils.isBlank(prefix)) {
            prefix = doc.getType();
        }
        int year = new GregorianCalendar().get(Calendar.YEAR);
        return prefix + year;
    }

    @Override
    public String createUID(DocumentModel doc) {
        String seqKey = getSequenceKey(doc);
        int index = getNext(doc);
        String n = String.format(FORMAT, Integer.valueOf(index));
        return seqKey + n;
    }

}
