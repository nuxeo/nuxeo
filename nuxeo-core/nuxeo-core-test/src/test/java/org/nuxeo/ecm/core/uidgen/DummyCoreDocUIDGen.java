/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.uidgen;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;

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
