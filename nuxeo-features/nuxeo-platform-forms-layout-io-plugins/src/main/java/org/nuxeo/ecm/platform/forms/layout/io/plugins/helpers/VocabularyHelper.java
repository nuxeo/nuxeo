/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Exports Vocabularies as translated SelectOptions
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class VocabularyHelper {

    private static final Log log = LogFactory.getLog(VocabularyHelper.class);

    public static final String PARENT_PROPERTY_NAME = "parent";

    public static final String LABEL_PROPERTY_NAME = "label";

    public static final String SUBDIRECTORY_SEPARATOR = "/";

    public static List<WidgetSelectOption> getVocabularySelectOptions(String dirName, String lang) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(dirName)) {
            String schema = ds.getDirectory(dirName).getSchema();
            DocumentModelList entries = session.getEntries();
            return convertToSelectOptions(entries, schema, dirName, lang);
        } catch (DirectoryException e) {
            log.error("Error while getting content of directory " + dirName, e);
            return Collections.emptyList();
        }
    }

    public static List<WidgetSelectOption> getChainSelectVocabularySelectOptions(String parentDirName,
            String childDirName, String lang) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        List<WidgetSelectOption> result = new ArrayList<WidgetSelectOption>();
        try (Session session = ds.open(parentDirName); Session subSession = ds.open(childDirName)) {
            String schema = ds.getDirectory(parentDirName).getSchema();
            String subSchema = ds.getDirectory(childDirName).getSchema();

            DocumentModelList entries = session.getEntries();
            for (DocumentModel entry : entries) {
                String itemValue = entry.getId();
                String itemLabel = (String) entry.getProperty(schema, LABEL_PROPERTY_NAME);
                if (lang != null) {
                    itemLabel = TranslationHelper.getTranslation(itemLabel, lang);
                }
                WidgetSelectOption selectOption = new WidgetSelectOptionImpl(itemLabel, itemValue);
                result.add(selectOption);

                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put(PARENT_PROPERTY_NAME, itemValue);
                DocumentModelList subEntries = subSession.query(filter, null);
                for (DocumentModel subEntry : subEntries) {
                    String subItemValue = itemValue + SUBDIRECTORY_SEPARATOR + subEntry.getId();
                    String subItemLabel = (String) subEntry.getProperty(subSchema, LABEL_PROPERTY_NAME);
                    if (lang != null) {
                        subItemLabel = TranslationHelper.getTranslation(subItemLabel, lang);
                    }
                    String subItemCompleteLabel = itemLabel + SUBDIRECTORY_SEPARATOR + subItemLabel;
                    WidgetSelectOption subSelectOption = new WidgetSelectOptionImpl(subItemCompleteLabel, subItemValue);
                    result.add(subSelectOption);
                }
            }
            return result;
        }
    }

    public static List<WidgetSelectOption> convertToSelectOptions(DocumentModelList entries, String schema,
            String directoryName, String lang) {
        List<WidgetSelectOption> res = new ArrayList<WidgetSelectOption>();
        for (DocumentModel entry : entries) {
            String itemValue = entry.getId();
            String itemLabel = itemValue;
            try {
                itemLabel = (String) entry.getProperty(schema, LABEL_PROPERTY_NAME);
                if (lang != null) {
                    itemLabel = TranslationHelper.getTranslation(itemLabel, lang);
                }
            } catch (PropertyException e) {
                if (lang != null) {
                    // try out l10n vocabulary structure
                    itemLabel = (String) entry.getProperty(schema, LABEL_PROPERTY_NAME + "_" + lang);
                }
            }
            WidgetSelectOption selectOption = new WidgetSelectOptionImpl(itemLabel, itemValue);
            res.add(selectOption);
        }
        return res;
    }

}
