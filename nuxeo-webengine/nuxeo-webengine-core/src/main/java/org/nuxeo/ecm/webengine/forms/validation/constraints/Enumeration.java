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

package org.nuxeo.ecm.webengine.forms.validation.constraints;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.nuxeo.ecm.webengine.forms.FormInstance;
import org.nuxeo.ecm.webengine.forms.validation.ErrorStatus;
import org.nuxeo.ecm.webengine.forms.validation.Field;
import org.nuxeo.ecm.webengine.forms.validation.Status;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Enumeration extends SimpleConstraint {

    protected Set<String> set;

    @Override
    public void doInit(Field field, String value, Object decodedValue) {
        set = new HashSet<String>();
        StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreTokens()) {
            set.add(tokenizer.nextToken());
        }
    }

    @Override
    public Status doValidate(FormInstance form, Field field, String rawValue, Object value) {
        assert set != null;
        return set.contains(rawValue) ? Status.OK
                : new ErrorStatus(field.getId());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        String[] ar = set.toArray(new String[set.size()]);
        int end = ar.length - 1;
        for (int i=0; i<end; i++) {
            String s = ar[i];
            buf.append(s).append(", ");
        }
        buf.append(ar[end]).append("}");
        return buf.toString();
    }

}
