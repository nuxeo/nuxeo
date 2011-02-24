package org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentDAO;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialDAO extends AbstractWebContentDAO<OpenSocialData> {

    @Override
    public OpenSocialData create(OpenSocialData data, String parentId,
            CoreSession session) throws Exception {
        return super.create(data, data.getGadgetName(), parentId, session);
    }

}
