package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

/**
 * Basic implementation of the {@link RequestFilterConfig} interface.
 *
 * @author tiry
 */
public class RequestFilterConfigImpl implements RequestFilterConfig {

    private static final long serialVersionUID = 1L;

    protected final boolean useTx;

    protected final boolean useSync;

    public RequestFilterConfigImpl(boolean useSync, boolean useTx) {
        this.useSync = useSync;
        this.useTx = useTx;
    }

    public boolean needSynchronization() {
        return useSync;
    }

    public boolean needTransaction() {
        return useTx;
    }

}
