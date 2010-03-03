package org.nuxeo.opensocial.container.factory.utils;

import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

public class GadgetsUtils {

    public static GadgetSpec getGadgetSpec(Gadget gadget) throws Exception {
        OpenSocialService service;
        service = Framework.getService(OpenSocialService.class);
        String gadgetDef = UrlBuilder.getGadgetDef(gadget.getName());
        GadgetSpecFactory gadgetSpecFactory = service.getGadgetSpecFactory();
        NxGadgetContext context = new NxGadgetContext(gadgetDef,
                gadget.getViewer(), gadget.getOwner());

        return gadgetSpecFactory.getGadgetSpec(context);
    }

}
