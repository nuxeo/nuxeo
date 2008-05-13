package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;

public class EventDescriptorPage implements Serializable {

    private static final long serialVersionUID = 876567561L;

    private final int pageIndex;

    private final boolean bHasMorePage;

    private EventDescriptor[] events;

    public EventDescriptorPage(EventDescriptor[] data, int pageIndex, boolean bHasMorePage)
    {

        this.pageIndex=pageIndex;
        this.bHasMorePage=bHasMorePage;
        this.events=data;

    }

    public int getPageIndex() {
        return pageIndex;
    }

    public boolean hasModePage() {
        return bHasMorePage;
    }

    public EventDescriptor[] getEvents() {
        return events;
    }

    public boolean getHasMorePage()
    {
        return bHasMorePage;
    }

}
