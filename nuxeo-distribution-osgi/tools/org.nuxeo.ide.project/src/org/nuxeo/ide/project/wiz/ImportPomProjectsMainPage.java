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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.nuxeo.ide.project.PomLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ImportPomProjectsMainPage extends WizardPage {

    protected WorkingSetGroup workingSetGroup;
    protected IStructuredSelection currentSelection;

    protected CheckboxTreeViewer projectsList;

    protected Text pomField;

    protected ImportPomProjectsMainPage(IStructuredSelection selection) {
        super("importTychoProjectMainPage");
        setTitle("Import Existing Tycho Projects");
        setDescription("You can also generate tycho projects for existing Nuxeo projects");
        currentSelection = selection;
    }

    public String getPath() {
        return pomField.getText().trim();
    }

    public IWorkingSet[] getWorkingSets() {
        return workingSetGroup.getSelectedWorkingSets();
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));


        createPomLocation(composite);
        createProjectsList(composite);
        createWorkingSetGroup(composite);

        setControl(composite);
        setPageComplete(false);
        Dialog.applyDialogFont(composite);
    }

    /**
     * @param workArea
     */
    private void createWorkingSetGroup(Composite workArea) {
        String[] workingSetIds = new String[] {"org.eclipse.ui.resourceWorkingSetPage",  //$NON-NLS-1$
                "org.eclipse.jdt.ui.JavaWorkingSetPage"};  //$NON-NLS-1$
        workingSetGroup = new WorkingSetGroup(workArea, currentSelection, workingSetIds);
    }



    private void createPomLocation(Composite workArea) {

        // project specification group
        Composite projectGroup = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        Label label = new Label(projectGroup, SWT.NONE);
        label.setText("Select parent pom:");
        // project location entry field
        pomField = new Text(projectGroup, SWT.BORDER);

        GridData directoryPathData = new GridData(SWT.FILL, SWT.NONE, true, false);
        directoryPathData.widthHint = new PixelConverter(pomField).convertWidthInCharsToPixels(25);
        pomField.setLayoutData(directoryPathData);

        // browse button
        Button browse = new Button(projectGroup, SWT.PUSH);
        browse.setText("Browse");
        setButtonLayoutData(browse);

        browse.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleLocationDirectoryButtonPressed();
            }
        });

    }

    /**
     * The browse button has been selected. Select the location.
     */
    protected void handleLocationDirectoryButtonPressed() {
        FileDialog dialog = new FileDialog(pomField.getShell(), SWT.SHEET);
        dialog.setText("Select a pom file");
        String path = dialog.open();
        dialog.setFilterNames(new String[] {"*.xml"});
        if (path != null) {
            pomField.setText(path);
            updateProjectsList();
        }
    }

    public CheckboxTreeViewer getProjectsList() {
        return projectsList;
    }

    protected ProjectEntry[] getAllProjects() {
        String path = getPath();
        try {
            if (path.length() > 0) {
                return getProjectEntries(path);
            }
        } catch (Throwable e) {
        }
        return new ProjectEntry[0];
    }

    protected ProjectEntry[] getImportableProjects() {
        String path = getPath();
        try {
            if (path.length() > 0) {
                return getImportableProjectEntries(path);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return new ProjectEntry[0];
    }

    /**
     * Create the selection buttons in the listComposite.
     *
     * @param listComposite
     */
    private void createSelectionButtons(Composite listComposite) {
        Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        buttonsComposite.setLayout(layout);

        buttonsComposite.setLayoutData(new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING));

        Button selectAll = new Button(buttonsComposite, SWT.PUSH);
        selectAll.setText("Select All");
        selectAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                projectsList.setSubtreeChecked(ImportPomProjectsMainPage.this, true);
                setPageComplete(projectsList.getCheckedElements().length > 0);
            }
        });
        Dialog.applyDialogFont(selectAll);
        setButtonLayoutData(selectAll);

        Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
        deselectAll.setText("Deselect All");
        deselectAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                projectsList.setCheckedElements(new Object[0]);
                setPageComplete(false);
            }
        });
        Dialog.applyDialogFont(deselectAll);
        setButtonLayoutData(deselectAll);

        /* Not more needed for now
        Button refresh = new Button(buttonsComposite, SWT.PUSH);
        refresh.setText("Generate");
        refresh.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String path = getPath();
                if (path.length() == 0) {
                   MessageDialog.openInformation(getShell(), "No pom selected", "You must first select a valid pom file.");
                   return;
                }
                try {
                    new ProjectGenerator(new File(path), false).run();
                } catch (Exception ee) {
                    ee.printStackTrace();
                    MessageDialog.openError(getShell(), "Error", "Failed to generate project metadata files");
                }
                updateProjectsList();
            }
        });
        Dialog.applyDialogFont(refresh);
        setButtonLayoutData(refresh);
        Button rebuild = new Button(buttonsComposite, SWT.PUSH);
        rebuild.setText("Rebuild All");
        rebuild.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String path = getPath();
                if (path.length() == 0) {
                   MessageDialog.openInformation(getShell(), "No pom selected", "You must first select a valid pom file.");
                   return;
                }
                try {
                    new ProjectGenerator(new File(path), true).run();
                } catch (Exception ee) {
                    ee.printStackTrace();
                    MessageDialog.openError(getShell(), "Error", "Failed to generate project metadata files");
                }
                updateProjectsList();
            }
        });

        Dialog.applyDialogFont(rebuild);
        setButtonLayoutData(rebuild);
        */
    }

    public void createProjectsList(Composite workArea) {
        Label title = new Label(workArea, SWT.NONE);
        title.setText("Projects");

        Composite listComposite = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.makeColumnsEqualWidth = false;
        listComposite.setLayout(layout);

        listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

        projectsList = new CheckboxTreeViewer(listComposite, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = new PixelConverter(projectsList.getControl()).convertWidthInCharsToPixels(25);
        gridData.heightHint = new PixelConverter(projectsList.getControl()).convertHeightInCharsToPixels(10);
        projectsList.getControl().setLayoutData(gridData);
        projectsList.setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return getImportableProjects();
            }
            @Override
            public Object[] getChildren(Object parentElement) {
                return null;
            }
            @Override
            public Object getParent(Object element) {
                return null;
            }
            @Override
            public void dispose() {
            }
            @Override
            public boolean hasChildren(Object element) {
                return false;
            }
            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {

            }
        });
        projectsList.setLabelProvider(new LabelProvider());
        projectsList.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isGrayed(Object element) {
                //return ((ProjectEntry)element).exists();
                return false;
            }
            @Override
            public boolean isChecked(Object element) {
                //return !((ProjectEntry)element).exists();
                return true;
            }
        });

        projectsList.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                setPageComplete(projectsList.getCheckedElements().length > 0);
            }
        });

        createSelectionButtons(listComposite);
        projectsList.setInput(this);
    }

    protected void updateProjectsList() {
        projectsList.setInput(this);
        setPageComplete(projectsList.getCheckedElements().length > 0);
    }


    protected ProjectEntry[] getImportableProjectEntries(String pomPath) throws Exception {
        ProjectEntry[] entries = getProjectEntries(pomPath);
        ArrayList<ProjectEntry> result = new ArrayList<ProjectEntry>();
        for (ProjectEntry entry : entries) {
            if (!entry.exists()) {
                result.add(entry);
            }
        }
        return result.toArray(new ProjectEntry[result.size()]);
    }

    protected ProjectEntry[] getProjectEntries(String pomPath) throws Exception {
        File pom = new File(pomPath);
        ArrayList<ProjectEntry> entries = new ArrayList<ProjectEntry>();
        collectProjects(pom.getParentFile(), pom, entries);
        Collections.sort(entries, new Comparator<ProjectEntry>() {
            @Override
            public int compare(ProjectEntry o1, ProjectEntry o2) {
                if (o1.exists != o2.exists) {
                    return o1.exists ? -1 : 1;
                }
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        return entries.toArray(new ProjectEntry[entries.size()]);
    }

    protected void collectProjects(File root, File pom, List<ProjectEntry> entries) throws Exception {
        PomLoader loader = new PomLoader(pom);
        if ("pom".equals(loader.getPackaging())) {
            for (File file : loader.getModuleFiles()) {
                collectProjects(file, new File(file, "pom.xml"), entries);
            }
        } else {
            try {
                entries.add(new ProjectEntry(root));
            } catch (Throwable t) {
                System.err.println("Invalid Eclipse project: "+root);
            }
        }
    }

}
