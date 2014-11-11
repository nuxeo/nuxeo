/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.text.MessageFormat;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MessagesMethod implements TemplateMethodModelEx {

    protected static final ResourceBundle NULL_BUNDLE = new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[0][0];
        }
    };

    protected ResourceBundle bundle;

    public MessagesMethod(ResourceBundle bundle) {
        setBundle(bundle);
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        if (this.bundle == null) {
            try {
                this.bundle = ResourceBundle.getBundle("messages");
            } catch (MissingResourceException e) {
                this.bundle = NULL_BUNDLE;
            }
        }
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public Object exec(List arguments) throws TemplateModelException {
        int size = arguments.size();
        if (size < 1) {
            throw new TemplateModelException("Invalid number of arguments for messages(key, args ..) method");
        }
        String key;
        SimpleScalar scalar = (SimpleScalar) arguments.get(0);
        if (scalar != null) {
            key = scalar.getAsString();
        } else {
            throw new TemplateModelException("the argument is not defined");
        }
        String value;
        try {
            value = bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
        if (size > 1) { // format the string using given args
            String[] args = new String[size-1];
            for (int i=0; i<args.length; i++) {
                args[i] = ((SimpleScalar) arguments.get(i + 1)).getAsString();
            }
            value = MessageFormat.format(value, (Object[]) args);
        }
        return value;
    }

}
