/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *     <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import org.nuxeo.ecm.platform.rendering.fm.i18n.ResourceComposite;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Message method that differs from the standard one as its second argument is
 * the locale.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 *
 */
public class LocaleMessagesMethod implements TemplateMethodModelEx {

    protected ResourceComposite bundle;

    public LocaleMessagesMethod(ResourceComposite bundle) {
        setBundle(bundle);
    }

    public void setBundle(ResourceComposite bundle) {
        this.bundle = bundle;
        if (this.bundle == null) {
            this.bundle = new ResourceComposite();
        }
    }

    public ResourceComposite getBundle() {
        return bundle;
    }

    public Object exec(List arguments) throws TemplateModelException {
        int size = arguments.size();
        if (size < 2) {
            throw new TemplateModelException(
                    "Invalid number of arguments for messages(key, local [, args ..]) method");
        }
        String key;
        SimpleScalar scalar = (SimpleScalar) arguments.get(0);
        if (scalar != null) {
            key = scalar.getAsString();
        } else {
            throw new TemplateModelException("the argument is not defined");
        }

        scalar = (SimpleScalar) arguments.get(1);
        String locale;
        if (scalar != null) {
            locale = scalar.getAsString();
        } else {
            throw new TemplateModelException("the argument is not defined");
        }

        String value;
        try {
            value = bundle.getString(key, new Locale(locale));
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
        if (size > 2) { // format the string using given args
            String[] args = new String[size - 2];
            for (int i = 0; i < args.length; i++) {
                args[i] = ((SimpleScalar) arguments.get(i + 2)).getAsString();
            }
            value = MessageFormat.format(value, (Object[]) args);
        }
        return value;
    }

}
