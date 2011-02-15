package org.nuxeo.opensocial.container.client.rpc.layout.result;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUIZoneResult implements Result {
    private static final long serialVersionUID = 1L;

    private YUIComponentZone component;

    public UpdateYUIZoneResult() {

    }

    public UpdateYUIZoneResult(YUIComponentZone component) {
        this.component = component;
    }

    public YUIComponentZone getZone() {
        return component;
    }
}
