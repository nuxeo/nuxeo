package org.nuxeo.opensocial.container.server.guice;

import org.nuxeo.opensocial.container.server.handler.InitApplicationHandler;
import org.nuxeo.opensocial.container.server.handler.layout.CreateYUIZoneHandler;
import org.nuxeo.opensocial.container.server.handler.layout.DeleteYUIZoneHandler;
import org.nuxeo.opensocial.container.server.handler.layout.UpdateYUILayoutBodySizeHandler;
import org.nuxeo.opensocial.container.server.handler.layout.UpdateYUILayoutFooterHandler;
import org.nuxeo.opensocial.container.server.handler.layout.UpdateYUILayoutHeaderHandler;
import org.nuxeo.opensocial.container.server.handler.layout.UpdateYUILayoutSideBarHandler;
import org.nuxeo.opensocial.container.server.handler.layout.UpdateYUIZoneHandler;
import org.nuxeo.opensocial.container.server.handler.webcontent.CreateWebContentHandler;
import org.nuxeo.opensocial.container.server.handler.webcontent.DeleteWebContentHandler;
import org.nuxeo.opensocial.container.server.handler.webcontent.UpdateAllWebContentsHandler;
import org.nuxeo.opensocial.container.server.handler.webcontent.UpdateWebContentHandler;

import net.customware.gwt.dispatch.server.guice.ActionHandlerModule;

/**
 * @author Stéphane Fourrier
 */
public class ServerModule extends ActionHandlerModule {
    @Override
    protected void configureHandlers() {
        bindHandler(CreateYUIZoneHandler.class);
        bindHandler(UpdateYUIZoneHandler.class);
        bindHandler(DeleteYUIZoneHandler.class);
        bindHandler(InitApplicationHandler.class);
        bindHandler(UpdateYUILayoutBodySizeHandler.class);
        bindHandler(UpdateYUILayoutSideBarHandler.class);
        bindHandler(UpdateYUILayoutHeaderHandler.class);
        bindHandler(UpdateYUILayoutFooterHandler.class);

        bindHandler(CreateWebContentHandler.class);
        bindHandler(UpdateAllWebContentsHandler.class);
        bindHandler(DeleteWebContentHandler.class);
        bindHandler(UpdateWebContentHandler.class);
    }
}
