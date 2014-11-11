package org.nuxeo.ecm.core.event.jms;

import java.rmi.dgc.VMID;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl;

/**
 * Default implementation for an {@link EventBundle} that need to be reconnected to a usable Session
 *
 * @author tiry
 *
 */
public class ReconnectedJMSEventBundle extends ReconnectedEventBundleImpl {

    private static final long serialVersionUID = 1L;

    protected JMSEventBundle jmsEventBundle;

    private static final Log log = LogFactory.getLog(ReconnectedJMSEventBundle.class);

    public ReconnectedJMSEventBundle(JMSEventBundle jmsEventBundle) {
        super();
        this.jmsEventBundle = jmsEventBundle;
    }

    @Override
    protected List<Event> getReconnectedEvents() {
        if (sourceEventBundle == null) {
            try {
                sourceEventBundle = jmsEventBundle.reconstructEventBundle(getReconnectedCoreSession(jmsEventBundle.getCoreInstanceName()));
            } catch (CannotReconstructEventBundle e) {
                log.error("Error while reconstructing Bundle from JMS", e);
                return null;
            }
        }
        return super.getReconnectedEvents();
    }

    @Override
    public String[] getEventNames() {
        List<Event> events = getReconnectedEvents();
        String[] names = new String[events.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = events.get(i).getName();
        }
        return names;
    }

    @Override
    public String getName() {
        return jmsEventBundle.getEventBundleName();
    }

    @Override
    public VMID getSourceVMID() {
        return jmsEventBundle.getSourceVMID();
    }

    @Override
    public boolean hasRemoteSource() {
        return !getSourceVMID().equals(EventServiceImpl.VMID);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean comesFromJMS() {
        return true;
    }
}
