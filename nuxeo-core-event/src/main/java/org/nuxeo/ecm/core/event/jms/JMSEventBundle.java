package org.nuxeo.ecm.core.event.jms;
import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;

public class JMSEventBundle implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected List<Map<String, Serializable>> serialisableEvents=null;

    protected String coreInstanceName;

    protected String eventBundleName;

    protected boolean isDocumentEventContext=false;

    protected VMID sourceVMID=null;

    private static final Log log = LogFactory.getLog(JMSEventBundle.class);

    public JMSEventBundle(EventBundle events) {

        eventBundleName = events.getName();
        sourceVMID = events.getSourceVMID();
        serialisableEvents = new ArrayList<Map<String,Serializable>>();

        for (Event event : events) {
            if (event.isLocal()) {
                // local event should not be exported to JMS
                continue;
            }

            CoreSession evtSession = event.getContext().getCoreSession();

            String repoName=null;
            if (evtSession!=null) {
                repoName = evtSession.getRepositoryName();
                if (coreInstanceName==null) {
                    coreInstanceName=repoName;
                }
            }

            Map<String,Serializable> serializableEvent = new HashMap<String, Serializable>();

            serializableEvent.put("name", event.getName());
            serializableEvent.put("time", Long.toString(event.getTime()));
            serializableEvent.put("contextProperties", (Serializable)filterContextProperties(event.getContext().getProperties()));
            if (evtSession!=null) {
                    serializableEvent.put("contextSessionId", evtSession.getSessionId());
            }
            serializableEvent.put("principal", event.getContext().getPrincipal().getName());

            serializableEvent.put("contextSessionRepositoryName", repoName);

            if (event.getContext() instanceof DocumentEventContext) {
                DocumentEventContext docContext = (DocumentEventContext) event.getContext();
                serializableEvent.put("isDocumentEventContext", true);
            } else {
                serializableEvent.put("isDocumentEventContext", false);
            }

            Object[] args = event.getContext().getArguments();
            List<Serializable> listArgs = new ArrayList<Serializable>();
            for (Object arg : args) {
                if (arg instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) arg;
                    String strRepresentation = doc.getRepositoryName() + ":" +  doc.getId();
                    listArgs.add("DOCREF:" + strRepresentation);
                }
                else if (arg instanceof Serializable) {
                    log.debug("Adding serializable argument of class " + arg.getClass().getCanonicalName());
                    listArgs.add((Serializable)arg);
                }
                else {
                    listArgs.add(null);
                }
            }

            serializableEvent.put("args", (Serializable)listArgs);
            serialisableEvents.add(serializableEvent);
        }

    }

    protected Map<String,Serializable> filterContextProperties(Map<String,Serializable> properties) {

        Map<String,Serializable> serializableProps = new HashMap<String, Serializable>();

        for (String key : properties.keySet()) {
            Object value = properties.get(key);
            if (value instanceof Serializable) {
                Serializable serializableValue = (Serializable) value;
                serializableProps.put(key, serializableValue);
            }
            else {
                log.error("ContextMap contains non serializable object under key " + key);
            }
        }
        return serializableProps;
    }

    public VMID getSourceVMID() {
        return sourceVMID;
    }

    public String getEventBundleName() {
        return eventBundleName;
    }

    public String getCoreInstanceName() {
        return coreInstanceName;
    }


    public class EventBundleRelayedViaJMS extends EventBundleImpl {
        public EventBundleRelayedViaJMS() {
            // init VMID
            super(sourceVMID);
        }
    }

    public EventBundle reconstructEventBundle(CoreSession session) throws CannotReconstructEventBundle {

        if ((session==null) || (!session.getRepositoryName().equals(coreInstanceName))) {
            throw new CannotReconstructEventBundle("This session can not be used on this Bundle");
        }
        EventBundle bundle = new EventBundleRelayedViaJMS();

        if (serialisableEvents==null) {
            return null;
        }

        for (Map<String,Serializable> evt : serialisableEvents) {

            String eventName = (String) evt.get("name");
            Long time = Long.parseLong(((String)evt.get("time")));

            EventContext ctx=null;

            Map<String, Serializable> ctxProperties = (Map<String, Serializable>)evt.get("contextProperties");
            Principal principal = new SimplePrincipal((String)evt.get("principal"));

            List<Serializable> listArgs = (List<Serializable>) evt.get("args");

            Object[] args = new Object[listArgs.size()];

            int idx=0;
            for (Serializable sArg : listArgs) {
                Object value = null;
                if (sArg == null) {
                    value=null;
                }
                else if (sArg instanceof String) {
                    String arg = (String) sArg;
                    if (arg.startsWith("DOCREF:")) {
                        String[] part = arg.split(":");
                        DocumentRef idRef = new IdRef(part[2]);
                        DocumentModel doc=null;
                        try {
                            doc = session.getDocument(idRef);
                        }
                        catch (ClientException e) {
                            // TODO
                        }
                        value=doc;
                    } else {
                        value=arg;
                    }
                }
                else {
                    value=(Object)sArg;
                }
                args[idx]=value;
                idx++;
            }


            if ((Boolean)evt.get("isDocumentEventContext")) {
                ctx = new DocumentEventContext(null,principal,(DocumentModel) args[0], (DocumentRef)args[1]);
                // XXX we loose other args ...
            }
            else {
                ctx = new EventContextImpl(null,principal,args);
            }

            ctx.setProperties(ctxProperties);

            Event e = new EventImpl(eventName, ctx, null, time);

            bundle.push(e);

        }
        return bundle;
    }
}
