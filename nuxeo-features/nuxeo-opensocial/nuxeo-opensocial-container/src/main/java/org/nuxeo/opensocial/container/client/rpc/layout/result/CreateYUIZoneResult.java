package org.nuxeo.opensocial.container.client.rpc.layout.result;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class CreateYUIZoneResult implements Result {
    private static final long serialVersionUID = 1L;

    private YUIComponentZone zone;

    @SuppressWarnings("unused")
    private CreateYUIZoneResult() {
    }

    public CreateYUIZoneResult(YUIComponentZone zone) {
        this.zone = zone;
    }

    public YUIComponentZone getZone() {
        return zone;
    }
}
