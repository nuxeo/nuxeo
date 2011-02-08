/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ide.project.wiz;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ImportPomProjects extends Wizard implements IImportWizard {

    protected IWorkbench workbench;
    protected IStructuredSelection selection;

    protected ImportPomProjectsMainPage mainPage;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }

    @Override
    public void addPages() {
        super.addPages();
        mainPage = new ImportPomProjectsMainPage(selection);
        addPage(mainPage);
    }


    @Override
    public boolean performFinish() {
        IWorkingSet[] sets = mainPage.getWorkingSets();
        if (sets!= null && sets.length == 0) {
            sets = null;
        }
        Object[] elements = mainPage.getProjectsList().getCheckedElements();
        if (elements != null && elements.length > 0) {
            try {
                ProjectEntry[] entries = new ProjectEntry[elements.length];
                System.arraycopy(elements, 0, entries, 0, elements.length);
                importProjectDescriptors(entries, sets);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public void importProjectDescriptors(final ProjectEntry[] entries, final IWorkingSet[] sets) throws CoreException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        // create the new project operation
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            protected void execute(IProgressMonitor monitor)
                    throws CoreException {
                monitor.beginTask("Importing projects", entries.length*3); //$NON-NLS-1$
                for (ProjectEntry entry : entries) {
                    IProject project = workspace.getRoot().getProject(entry.getDescription().getName());
                    project.create(entry.getDescription(), new SubProgressMonitor(monitor,
                            1));
                    monitor.worked(1);
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
                    project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));
                    if (sets != null) {
                        workbench.getWorkingSetManager().addToWorkingSets(project, sets);
                    }
                    monitor.worked(1);
                }
                monitor.done();
            }
        };

        Shell shell = getShell();

        // run the new project creation operation
        try {
            new ProgressMonitorDialog(shell).run(true, true, op);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            // ie.- one of the steps resulted in a core exception
            Throwable t = e.getTargetException();
            if (t instanceof CoreException) {
                if (((CoreException) t).getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
                    MessageDialog
                            .open(MessageDialog.ERROR,
                                    shell,
                                    DataTransferMessages.WizardExternalProjectImportPage_errorMessage,
                                    NLS.bind(
                                            DataTransferMessages.WizardExternalProjectImportPage_caseVariantExistsError,
                                            "TODO"),
                                    SWT.SHEET
                            );
                } else {
                    ErrorDialog
                            .openError(
                                    shell,
                                    DataTransferMessages.WizardExternalProjectImportPage_errorMessage,
                                    null, ((CoreException) t).getStatus());
                }
            }
        }

    }

}
