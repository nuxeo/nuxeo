package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.Event.Flag;

public class InlineEventContext extends EventContextImpl implements
        EventContext {

    private static final long serialVersionUID = 1L;

    protected boolean boundToCoreSession = false;

    public InlineEventContext(Principal principal, Map<String, Serializable> properties) {
        this(null, principal, properties);
    }

    public InlineEventContext(CoreSession session, Principal principal, Map<String, Serializable> properties) {
        super(session, principal);
        super.setProperties(properties);
        if (session!=null) {
            boundToCoreSession=true;
        }
        else {
            boundToCoreSession=false;
        }
    }

    @Override
    public Event event(String name) {
        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        if (!boundToCoreSession) {
            flags.add(Flag.INLINE);
        }
        return super.event(name, flags);
    }

}
