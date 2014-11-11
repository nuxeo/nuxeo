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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import org.apache.jackrabbit.name.QName;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class JCRName {

    public final QName qname;     // the JCR QName
    public final String uri;      // the uri (ex: 'http://nuxeo.org/ecm/names/')
    public final String prefix;   // the prefix (ex: 'ecm')
    public final String prefixwc; // the prefix with colon (ex: 'ecm:')
    public final String name;     // the local name (ex: 'root')
    public final String rawname;  // the pref + ':' +local name (ex: 'ecm:root')

    public JCRName(String name, String uri, String prefix) {
        this.uri = uri.intern();
        this.prefix = prefix.intern();
        this.name = name.intern();
        if (prefix.length() == 0) {
            prefixwc = "";
            rawname = (prefixwc + this.name).intern();
            qname = new QName(QName.NS_DEFAULT_URI, name);
        } else {
            prefixwc = prefix + ':';
            rawname = (prefixwc + this.name).intern();
            qname = new QName(uri, name);
        }
    }

}
