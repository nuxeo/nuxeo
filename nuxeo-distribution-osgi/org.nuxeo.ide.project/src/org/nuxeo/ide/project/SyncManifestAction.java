package org.nuxeo.ide.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.nuxeo.ide.project.utils.FileUtils;
import org.nuxeo.ide.project.utils.StringUtils;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class SyncManifestAction implements IWorkbenchWindowActionDelegate {
    private ISelection selection;
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public SyncManifestAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
	    syncManifest();
	}

	/**
	 * Selection in the workbench has been changed. We
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	    this.selection = selection;
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	@SuppressWarnings("unchecked")
    protected void syncManifest() {
	    if (selection == null || selection.isEmpty()) {
	        return;
	    }
	    ArrayList<String> hasErrors = new ArrayList<String>();
	    IStructuredSelection ss = ((IStructuredSelection)selection);
	    Iterator<IProject> it = ss.iterator();
	    while (it.hasNext()) {
	        IProject project = it.next();
	        IPath path = project.getLocation();
	        File file = path.toFile();
	        if (file.isDirectory() && file.getName().equals("osgi")) {
	            File root = file.getParentFile();
	            File mf2Update = new File(root, "src/main/resources/META-INF/MANIFEST.MF");
	            File mf = new File(file, "META-INF/MANIFEST.MF");
	            if (mf.isFile() && mf2Update.isFile()) {
	                //System.out.println("copy "+mf+" to "+mf2Update);
	                try {
	                    FileUtils.copyFile(mf, mf2Update);
	                } catch (Exception e) {
	                    hasErrors.add(project.getName());
	                }
	            }
	        }
	    }

	    if (!hasErrors.isEmpty()) {
	        MessageDialog.openWarning(window.getShell(), "Errors during synchronization", "Sync failed for projects: "+StringUtils.join(hasErrors, ','));
	    }

	}

}