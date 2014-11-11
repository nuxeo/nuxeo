/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationAdmin;
import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.events.EventHandler;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.events.operations.FireEvent;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionFilter;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionImpl;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl;
import org.nuxeo.ecm.automation.core.operations.FetchContextBlob;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.LogOperation;
import org.nuxeo.ecm.automation.core.operations.RestoreBlobInput;
import org.nuxeo.ecm.automation.core.operations.RestoreBlobInputFromScript;
import org.nuxeo.ecm.automation.core.operations.RestoreBlobsInput;
import org.nuxeo.ecm.automation.core.operations.RestoreBlobsInputFromScript;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInput;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInputFromScript;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentsInput;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentsInputFromScript;
import org.nuxeo.ecm.automation.core.operations.RunInputScript;
import org.nuxeo.ecm.automation.core.operations.RunScript;
import org.nuxeo.ecm.automation.core.operations.SetInputAsVar;
import org.nuxeo.ecm.automation.core.operations.SetVar;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.BlobToFile;
import org.nuxeo.ecm.automation.core.operations.blob.BlobToPDF;
import org.nuxeo.ecm.automation.core.operations.blob.ConcatenatePDFs;
import org.nuxeo.ecm.automation.core.operations.blob.ConvertBlob;
import org.nuxeo.ecm.automation.core.operations.blob.CreateBlob;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip;
import org.nuxeo.ecm.automation.core.operations.blob.GetAllDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.blob.PostBlob;
import org.nuxeo.ecm.automation.core.operations.blob.SetBlobFileName;
import org.nuxeo.ecm.automation.core.operations.business.BusinessCreateOperation;
import org.nuxeo.ecm.automation.core.operations.business.BusinessFetchOperation;
import org.nuxeo.ecm.automation.core.operations.business.BusinessUpdateOperation;
import org.nuxeo.ecm.automation.core.operations.document.AddEntryToMultiValuedProperty;
import org.nuxeo.ecm.automation.core.operations.document.AddPermission;
import org.nuxeo.ecm.automation.core.operations.document.CheckInDocument;
import org.nuxeo.ecm.automation.core.operations.document.CheckOutDocument;
import org.nuxeo.ecm.automation.core.operations.document.CopyDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateProxyLive;
import org.nuxeo.ecm.automation.core.operations.document.CreateVersion;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchByProperty;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.FilterDocuments;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChild;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentParent;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentVersions;
import org.nuxeo.ecm.automation.core.operations.document.GetLiveDocument;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.MoveDocument;
import org.nuxeo.ecm.automation.core.operations.document.MultiPublishDocument;
import org.nuxeo.ecm.automation.core.operations.document.PublishDocument;
import org.nuxeo.ecm.automation.core.operations.document.Query;
import org.nuxeo.ecm.automation.core.operations.document.ReloadDocument;
import org.nuxeo.ecm.automation.core.operations.document.RemoveDocumentACL;
import org.nuxeo.ecm.automation.core.operations.document.RemoveDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.RemoveEntryOfMultiValuedProperty;
import org.nuxeo.ecm.automation.core.operations.document.RemovePermission;
import org.nuxeo.ecm.automation.core.operations.document.RemoveProperty;
import org.nuxeo.ecm.automation.core.operations.document.RestoreVersion;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentACE;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentLifeCycle;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.operations.document.UnlockDocument;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.execution.RunDocumentChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunFileChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunInNewTransaction;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperation;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnListInNewTransaction;
import org.nuxeo.ecm.automation.core.operations.execution.SaveSession;
import org.nuxeo.ecm.automation.core.operations.login.LoginAs;
import org.nuxeo.ecm.automation.core.operations.login.Logout;
import org.nuxeo.ecm.automation.core.operations.stack.PopBlob;
import org.nuxeo.ecm.automation.core.operations.stack.PopBlobList;
import org.nuxeo.ecm.automation.core.operations.stack.PopDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PopDocumentList;
import org.nuxeo.ecm.automation.core.operations.stack.PullBlob;
import org.nuxeo.ecm.automation.core.operations.stack.PullBlobList;
import org.nuxeo.ecm.automation.core.operations.stack.PullDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PullDocumentList;
import org.nuxeo.ecm.automation.core.operations.stack.PushBlob;
import org.nuxeo.ecm.automation.core.operations.stack.PushBlobList;
import org.nuxeo.ecm.automation.core.operations.stack.PushDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PushDocumentList;
import org.nuxeo.ecm.automation.core.operations.traces.AutomationTraceGetOperation;
import org.nuxeo.ecm.automation.core.operations.traces.AutomationTraceToggleOperation;
import org.nuxeo.ecm.automation.core.rendering.operations.RenderDocument;
import org.nuxeo.ecm.automation.core.rendering.operations.RenderDocumentFeed;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo component that provide an implementation of the
 * {@link AutomationService} and handle extensions registrations.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(AutomationComponent.class);

    public static final String XP_OPERATIONS = "operations";

    public static final String XP_ADAPTERS = "adapters";

    public static final String XP_CHAINS = "chains";

    public static final String XP_EVENT_HANDLERS = "event-handlers";

    public static final String XP_CHAIN_EXCEPTION = "chainException";

    public static final String XP_AUTOMATION_FILTER = "automationFilter";

    protected OperationServiceImpl service;

    protected EventHandlerRegistry handlers;

    protected TracerFactory tracerFactory;

    @Override
    public void activate(ComponentContext context) throws Exception {
        service = new OperationServiceImpl();
        tracerFactory = new TracerFactory();
        bindManagement();
        // register built-in operations
        service.putOperation(FetchContextDocument.class);
        service.putOperation(FetchContextBlob.class);
        service.putOperation(SetVar.class);
        service.putOperation(PushDocument.class);
        service.putOperation(PushDocumentList.class);
        service.putOperation(PopDocument.class);
        service.putOperation(PopDocumentList.class);
        service.putOperation(SetInputAsVar.class);
        service.putOperation(RestoreDocumentInput.class);
        service.putOperation(RestoreDocumentsInput.class);
        service.putOperation(RestoreBlobInput.class);
        service.putOperation(RestoreBlobsInput.class);
        service.putOperation(RunScript.class);
        service.putOperation(RestoreDocumentInputFromScript.class);
        service.putOperation(RestoreDocumentsInputFromScript.class);
        service.putOperation(RestoreBlobInputFromScript.class);
        service.putOperation(RestoreBlobsInputFromScript.class);
        service.putOperation(RunOperation.class);
        service.putOperation(RunOperationOnList.class);
        service.putOperation(RunInNewTransaction.class);
        service.putOperation(RunDocumentChain.class);
        service.putOperation(RunFileChain.class);
        service.putOperation(CopyDocument.class);
        service.putOperation(CreateDocument.class);
        service.putOperation(CreateVersion.class);
        service.putOperation(CheckInDocument.class);
        service.putOperation(CheckOutDocument.class);
        service.putOperation(RestoreVersion.class);
        service.putOperation(DeleteDocument.class);
        service.putOperation(FetchDocument.class);
        service.putOperation(LockDocument.class);
        service.putOperation(Query.class);
        service.putOperation(FetchByProperty.class);
        service.putOperation(FilterDocuments.class);
        service.putOperation(UnlockDocument.class);
        service.putOperation(GetDocumentChildren.class);
        service.putOperation(GetDocumentChild.class);
        service.putOperation(GetDocumentParent.class);
        service.putOperation(GetDocumentVersions.class);
        service.putOperation(MoveDocument.class);
        service.putOperation(ReloadDocument.class);
        service.putOperation(SaveDocument.class);
        service.putOperation(SaveSession.class);
        service.putOperation(SetDocumentLifeCycle.class);
        service.putOperation(SetDocumentACE.class);
        service.putOperation(AddPermission.class);
        service.putOperation(RemovePermission.class);
        service.putOperation(RemoveDocumentACL.class);
        service.putOperation(SetDocumentProperty.class);
        service.putOperation(RemoveProperty.class);
        service.putOperation(UpdateDocument.class);
        service.putOperation(PublishDocument.class);
        service.putOperation(MultiPublishDocument.class);
        service.putOperation(GetDocumentBlob.class);
        service.putOperation(GetDocumentBlobs.class);
        service.putOperation(GetAllDocumentBlobs.class);
        service.putOperation(SetDocumentBlob.class);
        service.putOperation(PostBlob.class);
        service.putOperation(BlobToPDF.class);
        service.putOperation(ConcatenatePDFs.class);
        service.putOperation(ConvertBlob.class);
        service.putOperation(BlobToFile.class);
        service.putOperation(CreateBlob.class);
        service.putOperation(CreateZip.class);
        service.putOperation(AttachBlob.class);
        service.putOperation(SetBlobFileName.class);
        service.putOperation(RemoveDocumentBlob.class);
        service.putOperation(PushBlob.class);
        service.putOperation(PushBlobList.class);
        service.putOperation(PopBlob.class);
        service.putOperation(PopBlobList.class);

        service.putOperation(PullDocument.class);
        service.putOperation(PullDocumentList.class);
        service.putOperation(PullBlob.class);
        service.putOperation(PullBlobList.class);

        service.putOperation(FireEvent.class);
        service.putOperation(RunInputScript.class);

        service.putOperation(RenderDocument.class);
        service.putOperation(RenderDocumentFeed.class);

        service.putOperation(LoginAs.class);
        service.putOperation(Logout.class);

        service.putOperation(LogOperation.class);

        // From presales toolkit
        service.putOperation(AddEntryToMultiValuedProperty.class);
        service.putOperation(CreateProxyLive.class);
        service.putOperation(GetLiveDocument.class);
        service.putOperation(RemoveEntryOfMultiValuedProperty.class);

        // Business Operations
        service.putOperation(BusinessCreateOperation.class);
        service.putOperation(BusinessUpdateOperation.class);
        service.putOperation(BusinessFetchOperation.class);

        service.putOperation(RunOperationOnListInNewTransaction.class);

        // disabled operations
        // service.putOperation(RunScriptFile.class);

        // Trace related operations
        service.putOperation(AutomationTraceGetOperation.class);
        service.putOperation(AutomationTraceToggleOperation.class);

        handlers = new EventHandlerRegistry(service);
    }

    protected void bindManagement() throws MalformedObjectNameException,
            NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(tracerFactory, new ObjectName(
                "org.nuxeo.automation:name=tracerfactory"));
    }

    protected void unBindManagement() throws MalformedObjectNameException,
            NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException, InstanceNotFoundException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.unregisterMBean(new ObjectName(
                "org.nuxeo.automation:name=tracerfactory"));
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        service = null;
        handlers = null;
        tracerFactory = null;
        unBindManagement();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_OPERATIONS.equals(extensionPoint)) {
            OperationContribution opc = (OperationContribution) contribution;
            service.putOperation(opc.type, opc.replace,
                    contributor.getName().toString());
        } else if (XP_CHAINS.equals(extensionPoint)) {
            OperationChainContribution occ = (OperationChainContribution) contribution;
            // Register the chain
            OperationType docChainType = new ChainTypeImpl(service,
                    occ.toOperationChain(contributor.getContext().getBundle()),
                    occ);
            service.putOperation(docChainType, occ.replace);
        } else if (XP_CHAIN_EXCEPTION.equals(extensionPoint)) {
            ChainExceptionDescriptor chainExceptionDescriptor = (ChainExceptionDescriptor) contribution;
            ChainException chainException = new ChainExceptionImpl(
                    chainExceptionDescriptor);
            service.putChainException(chainException);
        } else if (XP_AUTOMATION_FILTER.equals(extensionPoint)) {
            AutomationFilterDescriptor automationFilterDescriptor = (AutomationFilterDescriptor) contribution;
            ChainExceptionFilter chainExceptionFilter = new ChainExceptionFilter(
                    automationFilterDescriptor);
            service.putAutomationFilter(chainExceptionFilter);
        } else if (XP_ADAPTERS.equals(extensionPoint)) {
            TypeAdapterContribution tac = (TypeAdapterContribution) contribution;
            service.putTypeAdapter(tac.accept, tac.produce,
                    tac.clazz.newInstance());
        } else if (XP_EVENT_HANDLERS.equals(extensionPoint)) {
            EventHandler eh = (EventHandler) contribution;
            if (eh.isPostCommit()) {
                handlers.putPostCommitEventHandler(eh);
            } else {
                handlers.putEventHandler(eh);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_OPERATIONS.equals(extensionPoint)) {
            service.removeOperation(((OperationContribution) contribution).type);
        } else if (XP_CHAINS.equals(extensionPoint)) {
            OperationChainContribution occ = (OperationChainContribution) contribution;
            service.removeOperationChain(occ.getId());
        } else if (XP_CHAIN_EXCEPTION.equals(extensionPoint)) {
            ChainExceptionDescriptor chainExceptionDescriptor = (ChainExceptionDescriptor) contribution;
            ChainException chainException = new ChainExceptionImpl(
                    chainExceptionDescriptor);
            service.removeExceptionChain(chainException);
        } else if (XP_AUTOMATION_FILTER.equals(extensionPoint)) {
            AutomationFilterDescriptor automationFilterDescriptor = (AutomationFilterDescriptor) contribution;
            AutomationFilter automationFilter = new ChainExceptionFilter(
                    automationFilterDescriptor);
            service.removeAutomationFilter(automationFilter);
        } else if (XP_ADAPTERS.equals(extensionPoint)) {
            TypeAdapterContribution tac = (TypeAdapterContribution) contribution;
            service.removeTypeAdapter(tac.accept, tac.produce);
        } else if (XP_EVENT_HANDLERS.equals(extensionPoint)) {
            EventHandler eh = (EventHandler) contribution;
            if (eh.isPostCommit()) {
                handlers.removePostCommitEventHandler(eh);
            } else {
                handlers.removeEventHandler(eh);
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == AutomationService.class || adapter == AutomationAdmin
                .class) {
            return adapter.cast(service);
        }
        if (adapter == EventHandlerRegistry.class) {
            return adapter.cast(handlers);
        }
        if (adapter == TracerFactory.class) {
            return adapter.cast(tracerFactory);
        }
        return null;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        super.applicationStarted(context);
        if (!tracerFactory.getRecordingState()) {
            log.info("You can activate automation trace mode to get more informations on automation executions");
        }
    }
}
