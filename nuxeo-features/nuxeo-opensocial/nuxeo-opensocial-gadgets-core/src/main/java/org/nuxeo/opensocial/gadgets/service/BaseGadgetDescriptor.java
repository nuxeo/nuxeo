package org.nuxeo.opensocial.gadgets.service;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.opensocial.gadgets.helper.GadgetI18nHelper;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public abstract class BaseGadgetDescriptor implements Serializable,
        GadgetDeclaration {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(BaseGadgetDescriptor.class);

    protected GadgetSpec cachedSpec = null;

    @Override
    public GadgetSpec getGadgetSpec() {
        if (cachedSpec != null) {
            return cachedSpec;
        }
        try {
            return Framework.getLocalService(GadgetService.class).getGadgetSpec(
                    this);
        } catch (Exception e) {
            log.error(
                    "Error while getting gadget spec for gadget " + getName(),
                    e);
            return null;
        }
    }

    protected String getDescriptionFromSpec() {
        if (getGadgetSpec() == null) {
            return null;
        }
        return getGadgetSpec().getModulePrefs().getDescription();
    }

    @Override
    public String getTitle() {
        if (getGadgetSpec() == null) {
            return getName();
        }
        return getGadgetSpec().getModulePrefs().getTitle();
    }

    @Override
    public String getTitle(Locale locale) {
        String name = getName();
        String title = GadgetI18nHelper.getI18nGadgetTitle(name, locale);
        if (title.equals(name)) {
            return getTitle();
        }
        return title;
    }

    @Override
    public String getDescription(Locale locale) {
        String name = getName();
        String description = GadgetI18nHelper.getI18nGadgetDescription(name, locale);
        if(description.equals(name)) {
            return getDescription();
        }
        return description;
    }

    @Override
    public String getAuthor() {
        if (getGadgetSpec() == null) {
            if (!isExternal()) {
                return "Nuxeo";
            }
            return null;
        }
        return getGadgetSpec().getModulePrefs().getAuthor();
    }

    @Override
    public String getScreenshot() {
        if (getGadgetSpec() == null) {
            return null;
        }
        Uri uri = getGadgetSpec().getModulePrefs().getScreenshot();
        if (uri == null) {
            return getThumbnail();
        } else {
            return uri.toString();
        }
    }

    @Override
    public String getThumbnail() {
        if (getGadgetSpec() == null) {
            return null;
        }
        Uri uri = getGadgetSpec().getModulePrefs().getThumbnail();
        if (uri == null) {
            return null;
        } else {
            return uri.toString();
        }
    }

    @Override
    public String getPublicGadgetDefinition() throws MalformedURLException {
        return getGadgetDefinition().toString();
    }

}
