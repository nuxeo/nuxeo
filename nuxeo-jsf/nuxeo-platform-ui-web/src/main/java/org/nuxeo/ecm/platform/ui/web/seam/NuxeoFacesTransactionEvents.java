/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.seam;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.BUILT_IN;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.transaction.FacesTransactionEvents;
import org.jboss.seam.transaction.Transaction;

/**
 * Custom listener to "transaction failed" event to customize behaviour (show the message only if there are no other
 * messages in the stack).
 * <p>
 * The default {@link FacesTransactionEvents} observer is disabled in components.xml.
 *
 * @see FacesTransactionEvents
 * @since 6.0
 */
@Name("nuxeoFacesTransactionEvents")
@Scope(APPLICATION)
@Install(precedence = BUILT_IN, classDependencies = "javax.faces.context.FacesContext")
@BypassInterceptors
@Startup
public class NuxeoFacesTransactionEvents {

    protected boolean transactionFailedMessageEnabled = true;

    @Observer(Transaction.TRANSACTION_FAILED)
    public void addTransactionFailedMessage(int status) {
        if (transactionFailedMessageEnabled) {
            // NXP-12483 + VEND-13: only add the default message if none was
            // already set
            FacesMessages fm = FacesMessages.instance();
            if (fm.getCurrentMessages().size() == 0 && fm.getLocalMessages().size() == 0) {
                StatusMessages.instance().addFromResourceBundleOrDefault(getTransactionFailedMessageSeverity(),
                        getTransactionFailedMessageKey(), getTransactionFailedMessage());
            }
        }
    }

    public String getTransactionFailedMessage() {
        return "Transaction failed";
    }

    public Severity getTransactionFailedMessageSeverity() {
        return Severity.WARN;
    }

    public String getTransactionFailedMessageKey() {
        return "org.jboss.seam.TransactionFailed";
    }

    public boolean isTransactionFailedMessageEnabled() {
        return transactionFailedMessageEnabled;
    }

    public void setTransactionFailedMessageEnabled(boolean enabled) {
        transactionFailedMessageEnabled = enabled;
    }

}
