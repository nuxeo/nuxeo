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
 *     <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.nl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import org.nuxeo.ecm.webengine.WebClassLoader;

/**
 * A resource bundle for webengine that holds a local, allows developpers to
 * change it from its api (setLocale) and that will delegate its method to the
 * correct resourcebundle according to the local chosen.
 * 
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 * 
 */
public class ResourceComposite extends ResourceBundle {
    HashMap<Locale, ResourceBundle> map = new HashMap<Locale, ResourceBundle>();

    ResourceBundle current = null;

    WebClassLoader cl;

    public ResourceComposite(WebClassLoader cl) {
        this.cl = cl;
    }

    /**
     * Set the local to be used.
     * 
     * @param locale
     */
    public void setLocale(Locale locale) {
        current = map.get(locale);
        if (current == null) {
            current = ResourceBundle.getBundle("messages", locale, cl);
            map.put(locale, current);
        }
    }

    @Override
    public Enumeration<String> getKeys() {
        if (current == null) {
            setLocale(Locale.getDefault());
        }
        return current.getKeys();
    }

    @Override
    protected Object handleGetObject(String key) {
        if (current == null) {
            setLocale(Locale.getDefault());
        }
        return current.getObject(key);

    }

}
