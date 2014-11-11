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

package org.nuxeo.ecm.platform.rendering.fm.i18n;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A resource bundle for Nuxeo Rendering that holds a map of locals, allows
 * developers to change it from its api (setLocale) and that will delegate its
 * method to the correct resourcebundle according to the local chosen.
 * 
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 * 
 */
public class ResourceComposite extends ResourceBundle {
    HashMap<Locale, ResourceBundle> map = new HashMap<Locale, ResourceBundle>();

    ResourceBundle current = null;

    ClassLoader cl;

    public ResourceComposite() {
        this.cl = null;
    }

    public ResourceComposite(ClassLoader cl) {
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
            if (cl == null) {
                current = ResourceBundle.getBundle("messages", locale);
            } else {
                current = ResourceBundle.getBundle("messages", locale, cl);
            }
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

    /**
     * Delegate getString using the resource bundle corresponding to the local
     * (create one if it doesn't exist).
     * 
     * @param key
     * @param locale
     * @return
     */
    public String getString(String key, Locale locale) {
        ResourceBundle bundle = map.get(locale);
        if (bundle == null) {
            if (cl == null) {
                bundle = ResourceBundle.getBundle("messages", locale);
            } else {
                bundle = ResourceBundle.getBundle("messages", locale, cl);
            }
            map.put(locale, bundle);
        }
        return (String) bundle.getString(key);
    }

}
