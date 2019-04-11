/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Messages {

    protected final Messages parent;

    protected final MessagesProvider provider;

    protected final Map<String, MessagesBundle> messages;

    protected MessagesBundle defaultMessages;

    protected static final String BUILT_IN_DEFAULT_LANG = "en";

    public Messages(Messages parent, MessagesProvider provider) {
        this.parent = parent;
        this.provider = provider;
        messages = new ConcurrentHashMap<>();
        String serverDefaultLang = Locale.getDefault().getLanguage();
        defaultMessages = getMessagesBundle(serverDefaultLang);
        if (defaultMessages == null) {
            defaultMessages = new MessagesBundle(null, new HashMap<String, String>());
        }
        if (defaultMessages.messages.size() == 0 && !BUILT_IN_DEFAULT_LANG.equals(serverDefaultLang)) {
            defaultMessages = getMessagesBundle(BUILT_IN_DEFAULT_LANG);
        }
    }

    public MessagesBundle getMessagesBundle() {
        return defaultMessages;
    }

    public MessagesBundle getMessagesBundle(String language) {
        if (language == null) {
            return defaultMessages;
        }
        MessagesBundle bundle = messages.get(language);
        if (bundle == null) {
            Map<String, String> map = provider.getMessages(language);
            if (map == null && defaultMessages != null) {
                return defaultMessages;
            }
            MessagesBundle parentBundle = parent != null ? parent.getMessagesBundle(language) : null;
            bundle = new MessagesBundle(parentBundle, map);
            messages.put(language, bundle);
        }
        return bundle;
    }

    public Object getObject(String key, String language) {
        MessagesBundle bundle = getMessagesBundle(language);
        if (bundle != null) {
            return bundle.getObject(key);
        }
        throw new MissingResourceException("Can't find resource for bundle " + this.getClass().getName() + ", key "
                + key, Messages.class.getName(), key);
    }

    public Object getObject(String key) {
        return getObject(key, null);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String language) {
        return (String) getObject(key, language);
    }

    public String[] getStringArray(String key) {
        return getStringArray(key, null);
    }

    public String[] getStringArray(String key, String language) {
        return (String[]) getObject(key, language);
    }

}
