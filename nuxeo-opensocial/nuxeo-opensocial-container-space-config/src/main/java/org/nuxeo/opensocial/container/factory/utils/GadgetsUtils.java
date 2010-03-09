package org.nuxeo.opensocial.container.factory.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

public class GadgetsUtils {

    public static GadgetSpec getGadgetSpec(Gadget gadget) throws Exception {
        OpenSocialService service;
        service = Framework.getService(OpenSocialService.class);
        String gadgetDef = UrlBuilder.getGadgetDef(gadget.getName());
        UserManager mgr = Framework.getService(UserManager.class);
        String pwd = null;
        if (!StringUtils.isEmpty(gadget.getViewer())) {
            DocumentModel model = mgr.getUserModel(gadget.getViewer());
            pwd = (String) model.getProperty("user", "password");
        }
        GadgetSpecFactory gadgetSpecFactory = service.getGadgetSpecFactory();
        NxGadgetContext context = new NxGadgetContext(gadgetDef,
                gadget.getViewer(), gadget.getOwner(), pwd);
        return gadgetSpecFactory.getGadgetSpec(context);
    }
}
