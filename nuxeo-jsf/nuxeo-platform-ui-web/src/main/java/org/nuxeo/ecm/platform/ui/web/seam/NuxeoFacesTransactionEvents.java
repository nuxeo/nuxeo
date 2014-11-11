/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Custom listener to "transaction failed" event to customize behaviour (show
 * the message only if there are no other messages in the stack).
 * <p>
 * The default {@link FacesTransactionEvents} observer is disabled in
 * components.xml.
 *
 * @see FacesTransactionEvents
 * @since 5.9.6
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
            if (fm.getCurrentMessages().size() == 0
                    && fm.getLocalMessages().size() == 0) {
                StatusMessages.instance().addFromResourceBundleOrDefault(
                        getTransactionFailedMessageSeverity(),
                        getTransactionFailedMessageKey(),
                        getTransactionFailedMessage());
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
        this.transactionFailedMessageEnabled = enabled;
    }

}
