package org.nuxeo.ecm.platform.forms.layout.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetSelectOptionImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Exports Vocabularies as translated SelectOptions
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.3
 */
public class VocabularyExporter {

    private static final Log log = LogFactory.getLog(VocabularyExporter.class);

    public static JSONArray getVocabulary(String dirName, String lang) {

        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = ds.open(dirName);
            String schema = ds.getDirectory(dirName).getSchema();
            DocumentModelList entries = session.getEntries();
            return exportToJson(entries, schema, lang);
        } catch (Exception e) {
            log.error("Error while getting content of directory " + dirName, e);
            return new JSONArray();
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                    log.error("Error while closing directory", e);
                }
            }
        }
    }

    public static JSONArray getVocabulary(String dirName, String subDirName,
            String lang) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        Session subSession = null;
        JSONArray result = new JSONArray();
        try {
            session = ds.open(dirName);
            String schema = ds.getDirectory(dirName).getSchema();
            subSession = ds.open(subDirName);
            String subSchema = ds.getDirectory(subDirName).getSchema();

            DocumentModelList entries = session.getEntries();
            for (DocumentModel entry : entries) {
                String itemValue = (String) entry.getProperty(schema, "id");
                String itemLabel = TranslationHelper.getTranslation(
                        (String) entry.getProperty(schema, "label"), lang);
                WidgetSelectOption selectOption = new WidgetSelectOptionImpl(
                        itemLabel, itemValue);
                result.add(exportToJson(selectOption));

                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put("parent", itemValue);
                DocumentModelList subEntries = subSession.query(filter, null);
                for (DocumentModel subEntry : subEntries) {
                    String subItemValue = itemValue + "/"
                            + (String) subEntry.getProperty(subSchema, "id");
                    String subItemLabel = itemLabel
                            + "/"
                            + TranslationHelper.getTranslation(
                                    (String) subEntry.getProperty(subSchema,
                                            "label"), lang);
                    WidgetSelectOption subSelectOption = new WidgetSelectOptionImpl(
                            subItemLabel, subItemValue);
                    result.add(exportToJson(subSelectOption));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error while getting content of directories " + dirName
                    + "/" + subDirName, e);
            return result;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                    log.error("Error while closing directory", e);
                }
            }
            if (subSession != null) {
                try {
                    subSession.close();
                } catch (DirectoryException e) {
                    log.error("Error while closing directory", e);
                }
            }
        }
    }

    public static JSONObject exportToJson(WidgetSelectOption selectOption) {
        JSONObject json = new JSONObject();
        Serializable value = selectOption.getValue();
        boolean isMulti = selectOption instanceof WidgetSelectOptions;
        if (isMulti) {
            json.element("multiple", true);
        } else {
            json.element("multiple", false);
        }
        if (value != null) {
            json.element("value", value);
        }
        String var = selectOption.getVar();
        if (var != null) {
            json.element("var", var);
        }
        String itemLabel = selectOption.getItemLabel();
        if (itemLabel != null) {
            json.element("itemLabel", itemLabel);
        }
        String itemValue = selectOption.getItemValue();
        if (itemValue != null) {
            json.element("itemValue", itemValue);
        }
        Serializable itemDisabled = selectOption.getItemDisabled();
        if (itemDisabled != null) {
            json.element("itemDisabled", itemDisabled);
        }
        Serializable itemRendered = selectOption.getItemRendered();
        if (itemRendered != null) {
            json.element("itemRendered", itemRendered);
        }
        if (isMulti) {
            WidgetSelectOptions selectOptions = (WidgetSelectOptions) selectOption;
            String ordering = selectOptions.getOrdering();
            if (ordering != null) {
                json.element("ordering", ordering);
            }
            Boolean caseSensitive = selectOptions.getCaseSensitive();
            if (caseSensitive != null) {
                json.element("caseSensitive", caseSensitive);
            }
        }
        return json;
    }

    public static JSONArray exportToJson(DocumentModelList entries,
            String schema, String lang) {
        JSONArray result = new JSONArray();
        for (DocumentModel entry : entries) {
            try {
                String itemValue = (String) entry.getProperty(schema, "id");
                String itemLabel = TranslationHelper.getTranslation(
                        (String) entry.getProperty(schema, "label"), lang);
                WidgetSelectOption selectOption = new WidgetSelectOptionImpl(
                        itemLabel, itemValue);
                result.add(exportToJson(selectOption));
            } catch (ClientException e) {
                log.error("Error while reading directory entries", e);
            }
        }
        return result;
    }

}
