package org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.server.utils.UrlBuilder;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DataType;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialAdapter extends AbstractWebContentAdapter implements
        WebContentAdapter<OpenSocialData> {

    public static final String GADGETS_PORT = "gadgets.port";

    public static final String GADGETS_HOST = "gadgets.host";

    public static final String GADGETS_PATH = "gadgets.path";

    public static final String NUXEO = "nuxeo";

    public static final String HTTP = "http://";

    public static final String HTTP_SEPARATOR = ":";

    public static final String SEPARATOR = "/";

    public OpenSocialAdapter(DocumentModel doc) {
        super(doc);
    }

    @SuppressWarnings("unchecked")
    public void feedFrom(OpenSocialData data) throws ClientException {
        super.setMetadataFrom(data);

        doc.setPropertyValue("wcopensocial:gadgetDefUrl", data.getGadgetDef());
        doc.setPropertyValue("wcopensocial:gadgetname", data.getGadgetName());

        List<Map<String, Serializable>> savedUserPrefs = (List<Map<String, Serializable>>) doc.getPropertyValue("wcopensocial:userPrefs");

        for (Entry<String, org.nuxeo.opensocial.container.shared.webcontent.UserPref> dataPrefs : data.getUserPrefs().entrySet()) {
            org.nuxeo.opensocial.container.shared.webcontent.UserPref dataPref = dataPrefs.getValue();

            if (dataPref.getActualValue() != null) {
                Map<String, Serializable> savedUserPref = new HashMap<String, Serializable>();
                savedUserPref.put("name", dataPref.getName());
                savedUserPref.put("value", dataPref.getActualValue());

                savedUserPrefs.add(savedUserPref);
            }
        }

        doc.setPropertyValue("wcopensocial:userPrefs",
                (Serializable) savedUserPrefs);

        setFrameUrlFor(data);
    }

    private void setFrameUrlFor(OpenSocialData data) throws ClientException {
        data.setFrameUrl(UrlBuilder.buildShindigUrl(data, getBaseUrl(), "ALL"));
    }

    public String getBaseUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(HTTP);
        sb.append(Framework.getProperty(GADGETS_HOST));
        sb.append(HTTP_SEPARATOR);
        sb.append(Framework.getProperty(GADGETS_PORT));
        sb.append(SEPARATOR);
        sb.append(NUXEO);
        sb.append(SEPARATOR);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public OpenSocialData getData() throws ClientException {
        OpenSocialData data = new OpenSocialData();

        super.getMetadataFor(data);

        data.setGadgetDef((String) doc.getPropertyValue("wcopensocial:gadgetDefUrl"));
        data.setGadgetName((String) doc.getPropertyValue("wcopensocial:gadgetname"));

        try {
            // We get the values from nuxeo for each saved preference
            List<Map<String, Serializable>> tempSavedUserPrefs = (List<Map<String, Serializable>>) doc.getPropertyValue("wcopensocial:userPrefs");
            Map<String, String> savedUserPrefs = new HashMap<String, String>();

            for (Map<String, Serializable> preference : tempSavedUserPrefs) {
                String name = (String) preference.get("name");
                String value = (String) preference.get("value");
                savedUserPrefs.put(name, value);
            }

            // Get Preferences from shindig and wrap them into the data
            // We don't use the class provided by shindig because of not
            // implemented classes in GWT
            OpenSocialService service = Framework.getService(OpenSocialService.class);
            GadgetSpecFactory gadgetSpecFactory = service.getGadgetSpecFactory();
            NXGadgetContext context = new NXGadgetContext(data.getGadgetDef());
            GadgetSpec gadgetSpec = gadgetSpecFactory.getGadgetSpec(context);

            Map<String, org.nuxeo.opensocial.container.shared.webcontent.UserPref> dataUserPrefs = new HashMap<String, org.nuxeo.opensocial.container.shared.webcontent.UserPref>();

            for (UserPref openSocialUserPref : gadgetSpec.getUserPrefs()) {
                org.nuxeo.opensocial.container.shared.webcontent.UserPref dataPref = new org.nuxeo.opensocial.container.shared.webcontent.UserPref(
                        openSocialUserPref.getName(),
                        DataType.valueOf(openSocialUserPref.getDataType().toString()));

                Map<String, String> enumValues = new LinkedHashMap<String, String>();

                for (EnumValuePair osprefValue : openSocialUserPref.getOrderedEnumValues()) {
                    enumValues.put(osprefValue.getDisplayValue(),
                            osprefValue.getValue());
                }

                dataPref.setEnumValues(enumValues);
                dataPref.setDisplayName(openSocialUserPref.getDisplayName());
                dataPref.setDefaultValue(openSocialUserPref.getDefaultValue());

                String name = dataPref.getName();

                if (savedUserPrefs.containsKey(name)) {
                    dataPref.setActualValue((String) savedUserPrefs.get(name));
                }

                dataUserPrefs.put(name, dataPref);
            }

            data.setUserPrefs(dataUserPrefs);
        } catch (Exception e) {
            throw new ClientException("Unable to get OpenSocial Service ...", e);
        }

        setFrameUrlFor(data);

        return data;
    }
}

class NXGadgetContext extends GadgetContext {
    protected String url;

    public NXGadgetContext(String url) {
        super();
        this.url = url;
    }

    @Override
    public Uri getUrl() {
        return Uri.parse(url);
    }

    @Override
    public boolean getIgnoreCache() {
        return false;
    }

    @Override
    public String getContainer() {
        return "default";
    }
}
