/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Message method that differs from the standard one as its second argument is the locale.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
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

    @Override
    public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
        int size = arguments.size();
        if (size < 2) {
            throw new TemplateModelException("Invalid number of arguments for messages(key, local [, args ..]) method");
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
