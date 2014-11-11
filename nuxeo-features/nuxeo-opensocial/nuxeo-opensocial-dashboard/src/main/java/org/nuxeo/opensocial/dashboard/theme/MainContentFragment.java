package org.nuxeo.opensocial.dashboard.theme;

import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;

public class MainContentFragment extends AbstractFragment {

    @Override
    public Model getModel() throws ModelException {
        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        if (documentManager != null) {
            return getModel(documentManager);

        }
        return null;
    }

    private Model getModel(CoreSession session) {
        // TODO Auto-generated method stub
        MainContentModel model = new MainContentModel();

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        model.setAnonymous(principal.isAnonymous());

        return model;

    }

}
