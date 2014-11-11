package org.nuxeo.opensocial.dashboard.theme;

import java.util.List;

import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;

public class GadgetManagerFragment extends AbstractFragment {

    @Override
    public Model getModel() throws ModelException {
        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        if (documentManager != null) {
            return getModel(documentManager);

        }
        return null;
    }

    public Model getModel(CoreSession session) throws ModelException {

        try {
            GadgetService service = Framework.getService(GadgetService.class);
            List<String> categories = service.getGadgetCategory();
            List<GadgetDeclaration> gadgets = service.getGadgetList();
            GadgetManagerModel model = new GadgetManagerModel(categories,
                    gadgets);

            NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
            model.setAnonymous(principal.isAnonymous());

            return model;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
