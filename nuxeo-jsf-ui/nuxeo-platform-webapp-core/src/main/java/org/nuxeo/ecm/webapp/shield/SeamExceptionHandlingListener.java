/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     arussel
 */
package org.nuxeo.ecm.webapp.shield;

import java.io.IOException;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.mock.MockExternalContext;
import org.jboss.seam.mock.MockFacesContext;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Plays with conversations, trying to rollback transaction.
 *
 * @author arussel
 */
public class SeamExceptionHandlingListener implements ExceptionHandlingListener {

    private static final Log log = LogFactory.getLog(SeamExceptionHandlingListener.class);

    /**
     * Initiates a mock faces context when needed and tries to restore current conversation
     */
    @Override
    public void beforeSetErrorPageAttribute(Throwable t, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        // cut/paste from seam Exception filter.
        // we recreate the seam context to be able to use the messages.

        // patch following https://jira.jboss.org/browse/JBPAPP-1427
        // Ensure that the call in which the exception occurred was cleaned up
        // - it might not be, and there is no harm in trying

        // we keep around the synchronization component because
        // SeSynchronizations expects it to be unchanged when called by
        // the finally block of UTTransaction.rollback
        String SEAM_TX_SYNCS = "org.jboss.seam.transaction.synchronizations";
        Object syncs = null;
        if (Contexts.isEventContextActive()) {
            syncs = Contexts.getEventContext().get(SEAM_TX_SYNCS);
        }

        Lifecycle.endRequest();

        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            // the FacesContext is gone - create a fake one for Redirect and
            // HttpError to call
            MockFacesContext mockContext = createFacesContext(request, response);
            mockContext.setCurrent();
            facesContext = mockContext;
            log.debug("Created mock faces context for exception handling");
        } else {
            // do not use a mock faces context when a real one still exists:
            // when released, the mock faces context is nullified, and this
            // leads to errors like in the following stack trace:
            /**
             * java.lang.NullPointerException: FacesContext is null at org.ajax4jsf
             * .context.AjaxContext.getCurrentInstance(AjaxContext.java:159) at
             * org.ajax4jsf.context.AjaxContext.getCurrentInstance(AjaxContext. java:144) at
             * org.ajax4jsf.component.AjaxViewRoot.getViewId(AjaxViewRoot .java:580) at
             * com.sun.faces.lifecycle.Phase.doPhase(Phase.java:104)
             */
            log.debug("Using existing faces context for exception handling");
        }

        // NXP-5998: conversation initialization seems to be already handled by
        // SeamPhaseListener => do not handle conversation as otherwise it'll
        // stay unlocked.

        // if the event context was cleaned up, fish the conversation id
        // directly out of the ServletRequest attributes, else get it from
        // the event context
        // Manager manager = Contexts.isEventContextActive() ? (Manager)
        // Contexts.getEventContext().get(
        // Manager.class)
        // : (Manager)
        // request.getAttribute(Seam.getComponentName(Manager.class));
        // String conversationId = manager == null ? null
        // : manager.getCurrentConversationId();

        FacesLifecycle.beginExceptionRecovery(facesContext.getExternalContext());

        // restore syncs
        if (syncs != null) {
            Contexts.getEventContext().set(SEAM_TX_SYNCS, syncs);
        }

        // If there is an existing long-running conversation on
        // the failed request, propagate it
        // if (conversationId == null) {
        // Manager.instance().initializeTemporaryConversation();
        // } else {
        // ConversationPropagation.instance().setConversationId(conversationId);
        // Manager.instance().restoreConversation();
        // }
    }

    @Override
    public void beforeForwardToErrorPage(Throwable t, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        // nothing
    }

    /**
     * Rollbacks transaction if necessary
     */
    @Override
    public void startHandling(Throwable t, HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        if (TransactionHelper.isTransactionActive()) {
            TransactionHelper.setTransactionRollbackOnly();
        }
    }

    /**
     * Cleans up context created in
     * {@link #beforeSetErrorPageAttribute(Throwable, HttpServletRequest, HttpServletResponse)} when needed.
     */
    @Override
    public void afterDispatch(Throwable t, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        FacesContext context = FacesContext.getCurrentInstance();
        // XXX: it's not clear what should be done here: current tests
        // depending on the faces context just allow to avoid errors after
        if (context instanceof MockFacesContext) {
            // do not end the request if it's a real faces context, otherwise
            // we'll get the following stack trace:
            /**
             * java.lang.IllegalStateException: No active application scope at
             * org.jboss.seam.core.Init.instance(Init.java:76) at org.jboss.seam.
             * jsf.SeamPhaseListener.handleTransactionsAfterPhase( SeamPhaseListener.java:330) at
             * org.jboss.seam.jsf.SeamPhaseListener .afterServletPhase(SeamPhaseListener.java:241) at org.jboss.seam.jsf
             * .SeamPhaseListener.afterPhase(SeamPhaseListener.java:192) at
             * com.sun.faces.lifecycle.Phase.handleAfterPhase(Phase.java:175) at
             * com.sun.faces.lifecycle.Phase.doPhase(Phase.java:114) at com.sun.faces
             * .lifecycle.LifecycleImpl.render(LifecycleImpl.java:139)
             */
            FacesLifecycle.endRequest(context.getExternalContext());
            // do not release an actual FacesContext that we did not create,
            // otherwise we get the following stack trace:
            /**
             * java.lang.IllegalStateException at com.sun.faces.context.FacesContextImpl
             * .assertNotReleased(FacesContextImpl.java:395) at com.sun.faces.context
             * .FacesContextImpl.getExternalContext(FacesContextImpl.java:147) at
             * com.sun.faces.util.RequestStateManager.getStateMap( RequestStateManager.java:276) at
             * com.sun.faces.util.RequestStateManager .remove(RequestStateManager.java:243) at
             * com.sun.faces.context.FacesContextImpl .release(FacesContextImpl.java:345)
             */
            context.release();
        }
    }

    protected MockFacesContext createFacesContext(HttpServletRequest request, HttpServletResponse response) {
        MockFacesContext mockFacesContext = new MockFacesContext(new MockExternalContext(
                request.getSession().getServletContext(), request, response), new MockApplication());
        mockFacesContext.setViewRoot(new UIViewRoot());
        return mockFacesContext;
    }

    @Override
    public void responseComplete() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.responseComplete();
        } else {
            log.error("Cannot set response complete: faces context is null");
        }
    }

}
