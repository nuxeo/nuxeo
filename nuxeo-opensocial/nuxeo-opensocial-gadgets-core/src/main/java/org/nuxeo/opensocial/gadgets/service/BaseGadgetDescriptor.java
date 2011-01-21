package org.nuxeo.opensocial.gadgets.service;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public abstract class BaseGadgetDescriptor implements Serializable, GadgetDeclaration  {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(BaseGadgetDescriptor.class);

    protected GadgetSpec cachedSpec=null;

    public GadgetSpec getGadgetSpec() {
        if (cachedSpec!=null) {
            return cachedSpec;
        }
        try {
            return Framework.getLocalService(GadgetService.class).getGadgetSpec(this);
        } catch (Exception e) {
            log.error("Error while getting gagget spec for gadget " + getName(), e);
            return null;
        }
    }

    protected String getDescriptionFromSpec() {
        if (getGadgetSpec()==null) {
            return null;
        }
        return getGadgetSpec().getModulePrefs().getDescription();
    }

    public String getTitle() {
        if (getGadgetSpec()==null) {
            return getName();
        }
        return getGadgetSpec().getModulePrefs().getTitle();
    }

    public String getAuthor() {
        if (getGadgetSpec()==null) {
            if (!isExternal()) {
                return "Nuxeo";
            }
            return null;
        }
        return getGadgetSpec().getModulePrefs().getAuthor();
    }

    public String getScreenshot() {
        if (getGadgetSpec()==null) {
            return null;
        }
        Uri uri = getGadgetSpec().getModulePrefs().getScreenshot();
        if (uri==null) {
            return getThumbnail();
        } else {
            return uri.toString();
        }
    }

    public String getThumbnail() {
        if (getGadgetSpec()==null) {
            return null;
        }
        Uri uri =  getGadgetSpec().getModulePrefs().getThumbnail();
        if (uri==null) {
            return null;
        } else {
            return uri.toString();
        }
    }



}
