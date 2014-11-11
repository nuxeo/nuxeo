/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A resource bundle for Nuxeo Rendering that holds a map of locals, allows
 * developers to change it from its api (setLocale) and that will delegate its
 * method to the correct resource bundle according to the local chosen.
 *
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 */
public class ResourceComposite extends ResourceBundle {

    final Map<Locale, ResourceBundle> map = new HashMap<Locale, ResourceBundle>();

    final ClassLoader cl;

    ResourceBundle current;

    public ResourceComposite() {
        cl = null;
    }

    public ResourceComposite(ClassLoader cl) {
        this.cl = cl;
    }

    /**
     * Set the locale to be used.
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
     * Delegates getString using the resource bundle corresponding to the local
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
        return bundle.getString(key);
    }

}
