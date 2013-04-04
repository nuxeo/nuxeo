package org.nuxeo.ecm.platform.publisher.web;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

@SuppressWarnings("deprecation")
public class DummyPublishActions extends AbstractPublishActions {

    public DummyPublishActions(CoreSession documentManager, ResourcesAccessor resourcesAccessor) {
        this.documentManager = documentManager;
        this.resourcesAccessor = resourcesAccessor;
    }

}
