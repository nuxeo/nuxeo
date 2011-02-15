package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.CreateYUIZoneResult;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;

/**
 * @author Stéphane Fourrier
 */
public class CreateYUIZone extends AbstractAction<CreateYUIZoneResult> {

    private static final long serialVersionUID = 1L;

    private YUIComponentZone zone;

    private int zoneIndex;

    @SuppressWarnings("unused")
    private CreateYUIZone() {
        super();
    }

    public CreateYUIZone(ContainerContext containerContext,
            final YUIComponentZone zone, final int zoneIndex) {
        super(containerContext);
        this.zone = zone;
        this.zoneIndex = zoneIndex;
    }

    public YUIComponentZone getZone() {
        return zone;
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

}
