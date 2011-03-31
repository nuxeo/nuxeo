package org.nuxeo.ecm.platform.jbpm.core.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.runtime.api.Framework;

/**
 * Action handler that fire an event using EventProducer
 *
 * @see org.nuxeo.ecm.core.event.EventProducer
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class SendEventActionHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    protected static final String INITIATOR = "initiator";

    protected static final String PARTICIPANTS = "participants";

    private static final Log log = LogFactory.getLog(AbstractJbpmHandlerHelper.class);

    protected String eventName;

    protected String recipients;

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        assert eventName != null;
        assert recipients != null;

        if (nuxeoHasStarted()) {
            DocumentModel documentModel = (DocumentModel) getTransientVariable(JbpmService.VariableName.document.name());
            NuxeoPrincipal principal = (NuxeoPrincipal) getTransientVariable(JbpmService.VariableName.principal.name());
            if (documentModel == null) {
                return;
            }

            CoreSession coreSession = getCoreSession(principal);
            try {
                EventProducer eventProducer = getEventProducerService();
                DocumentEventContext ctx = new DocumentEventContext(
                        coreSession, principal, documentModel);
                ctx.setProperty("recipients", getRecipients());
                eventProducer.fireEvent(ctx.newEvent(eventName));
            } finally {
                closeCoreSession(coreSession);
            }
        }
    }

    protected EventProducer getEventProducerService() throws ClientException {
        try {
            return Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected String[] getRecipients() {
        List<String> recipientsVal = new ArrayList<String>();
        if (recipients.equals(PARTICIPANTS)) {
            VirtualTaskInstance participant = (VirtualTaskInstance) executionContext.getContextInstance().getVariable(
                    JbpmService.VariableName.participants.name());
            if (participant != null) {
                recipientsVal.addAll(participant.getActors());
            }
        } else if (recipients.equals(INITIATOR)) {
            String actorId = (String) executionContext.getContextInstance().getVariable(
                    JbpmService.VariableName.initiator.name());
            recipientsVal.add(actorId);
        } else {
            log.info("Unknown recipient : " + recipients);
        }
        return recipientsVal.toArray(new String[] {});
    }
}
