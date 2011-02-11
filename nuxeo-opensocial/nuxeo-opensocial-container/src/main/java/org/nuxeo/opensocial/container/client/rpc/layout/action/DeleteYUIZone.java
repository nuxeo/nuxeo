package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.DeleteYUIZoneResult;

/**
 * @author Stéphane Fourrier
 */
public class DeleteYUIZone extends AbstractAction<DeleteYUIZoneResult> {

    private static final long serialVersionUID = 1L;

    private int zoneIndex;

    @SuppressWarnings("unused")
    private DeleteYUIZone() {
        super();
    }

    public DeleteYUIZone(ContainerContext containerContext, final int zoneIndex) {
        super(containerContext);
        this.zoneIndex = zoneIndex;
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

}
