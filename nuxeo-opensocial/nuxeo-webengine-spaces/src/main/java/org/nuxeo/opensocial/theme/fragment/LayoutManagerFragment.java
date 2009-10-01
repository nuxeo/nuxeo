package org.nuxeo.opensocial.theme.fragment;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.opensocial.theme.model.LayoutManagerModel;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;


public class LayoutManagerFragment extends AbstractFragment{

  @Override
  public Model getModel() throws ModelException {
    if (WebEngine.getActiveContext() != null) {
      WebContext ctx = WebEngine.getActiveContext();
      CoreSession session = ctx.getCoreSession();

      return getModel(session);

    }
    return null;
  }


  public Model getModel(CoreSession session) throws ModelException {

    LayoutManagerModel model = new LayoutManagerModel();

    NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
    model.setAnonymous(principal.isAnonymous());

    return model;
  }

}
