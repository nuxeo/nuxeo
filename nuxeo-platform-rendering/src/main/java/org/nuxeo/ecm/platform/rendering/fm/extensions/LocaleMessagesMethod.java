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
 * Message method that differe from the standard one as its second argument is
 * the local.
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

        String locale;
        scalar = (SimpleScalar) arguments.get(1);
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
