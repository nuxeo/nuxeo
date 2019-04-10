/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.context.extensions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.context.DocumentWrapper;

public class ContextFunctions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    public ContextFunctions(DocumentModel doc, DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
    }

    public String getVocabularyTranslatedLabel(String voc_name, String key, String lang) {

        String labelKey = getVocabularyLabel(voc_name, key);
        if (labelKey == null) {
            return null;
        }

        Locale locale = new Locale(lang);
        if (voc_name.contains("/") && labelKey.contains(" / ")) {
            String[] parts = labelKey.split(" \\/ ");

            String result = "";
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    result = result + " / ";
                }
                try {
                    result = result + I18NUtils.getMessageString("messages", parts[i], null, locale);
                } catch (MissingResourceException e) {
                    result = result + parts[i];
                }
            }
            return result;
        } else {
            try {
                return I18NUtils.getMessageString("messages", labelKey, null, locale);
            } catch (MissingResourceException e) {
                return labelKey;
            }
        }
    }

    public String getVocabularyLabel(String voc_name, String key) {

        DirectoryService ds = Framework.getService(DirectoryService.class);
        if (ds == null) {
            return key;
        }

        List<String> vocs = new ArrayList<String>();
        List<String> keys = new ArrayList<String>();
        if (voc_name.contains("/")) {
            String[] parts = voc_name.split("\\/");
            for (String part : parts) {
                vocs.add(part);
            }
            parts = key.split("\\/");
            for (String part : parts) {
                keys.add(part);
            }
        } else {
            vocs.add(voc_name);
            keys.add(key);
        }

        List<String> values = new ArrayList<String>();

        for (int i = 0; i < vocs.size(); i++) {
            String voc = vocs.get(i);
            String keyValue = keys.get(i);
            if (ds.getDirectoryNames().contains(voc)) {
                Directory dir = ds.getDirectory(voc);
                String schema = dir.getSchema();
                if ("vocabulary".equals(schema) || "xvocabulary".equals(schema)) {
                    try (Session session = dir.getSession()) {
                        DocumentModel entry = session.getEntry(keyValue);
                        if (entry != null) {
                            values.add((String) entry.getProperty(schema, "label"));
                        }
                    }
                }
            }
        }
        if (values.size() == 0) {
            return key;
        } else if (values.size() == 1) {
            return values.get(0);
        } else {
            String result = "";
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    result = result + " / ";
                }
                result = result + values.get(i);
            }
            return result;
        }
    }

    public String formatDate(Object calendar) {
        return formatDate(calendar, "MM/dd/yyyy");
    }

    public String formatDateTime(Object calendar) {
        return formatDate(calendar, "MM/dd/yyyy HH:mm:ss");
    }

    public String formatTime(Object calendar) {
        return formatDate(calendar, "HH:mm:ss");
    }

    public String formatDate(Object calendar, String format) {
        Date dt = null;
        if (calendar instanceof Calendar) {
            dt = ((Calendar) calendar).getTime();
        } else if (calendar instanceof Date) {
            dt = (Date) calendar;
        }

        if (dt == null) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);

        return dateFormat.format(calendar);
    }

    public NuxeoPrincipal getNuxeoPrincipal(String username) {
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager == null) {
            return null;
        }
        return userManager.getPrincipal(username);
    }
}
