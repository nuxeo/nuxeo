/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.navigation;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.STATELESS;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.navigation.Pages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.pathelements.ArchivedVersionsPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.DocumentPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.PathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.TextPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.VersionDocumentPathElement;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * The new approach: keep all selected documents into a list. Add new document
 * to the list each time a new document is selected, after rebuilding the path.
 *
 * <p>
 * Algorithm for rebuilding the path:
 * <p>
 * d1 -> d2 -> d3 -> d4
 * <p>
 * A new document is selected, which is a child of d2, named d2.5. We need to
 * add d2.5 to the list after all unneeded documents have been removed to the
 * list. In the end the list should look like this: d1 -> d2 -> d2.5. We need to
 * remove all the documents in the list after d2, and add d2.5 to the list.
 *
 * TODO: fix bug when selecting an item located on a different branch than the
 * current one so that its parent is not found in the current branch
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("breadcrumbActions")
@Scope(STATELESS)
@Install(precedence = FRAMEWORK)
public class BreadcrumbActionsBean implements BreadcrumbActions {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(BreadcrumbActionsBean.class);

    @In(create = true)
    NavigationContext navigationContext;

    @In(create = true, required = false)
    private CoreSession documentManager;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    private static final String VIEW_DOMAIN_OUTCOME = "view_domains";

    private static final String BREADCRUMB_PREFIX = "breadcrumb";

    private static final String PATH_SHORTCUT = "....";

    // minimum path segments that must be displayed without shrinking
    protected static int MIN_PATH_SEGMENTS_LEN = 4;

    // maximum length path that can be displayed without shrinking
    protected static int MAX_PATH_CHAR_LEN = 80;

    public String navigateToParent() throws ClientException {
        List<PathElement> documentsFormingPath = getBackendPath();
        int nbDocInList = documentsFormingPath.size();
        // if there is the case, remove the starting
        if (nbDocInList > 0
                && documentsFormingPath.get(0).getName().equals(PATH_SHORTCUT)) {
            documentsFormingPath.remove(0);
        }

        nbDocInList = documentsFormingPath.size();

        if (nbDocInList == 0) {
            return "view_servers";
        }

        String outcome;
        if (nbDocInList > 1) {
            PathElement parentPathElement = documentsFormingPath.get(nbDocInList - 2);
            outcome = navigateToPathElement(parentPathElement);
        } else {
            PathElement pathElement = documentsFormingPath.get(0);
            if (pathElement instanceof TextPathElement) {
                DocumentModel currentDocument = navigationContext.getCurrentDocument();
                if (currentDocument == null) {
                    return "view_servers";
                } else {
                    return navigationContext.navigateToDocument(currentDocument);
                }
            }

            DocumentPathElement currentPathELement = (DocumentPathElement) pathElement;
            DocumentModel doc = currentPathELement.getDocumentModel();

            if (documentManager.hasPermission(doc.getParentRef(),
                    SecurityConstants.READ)) {
                outcome = navigationContext.navigateToRef(doc.getParentRef());
            } else {
                outcome = navigateToPathElement(currentPathELement);
            }
            if (navigationContext.getCurrentDocument().getType().equals(
                    "CoreRoot")) {
                outcome = VIEW_DOMAIN_OUTCOME;
            }
        }
        return outcome;
    }

    protected String navigateToPathElement(PathElement pathElement)
            throws ClientException {
        // the bijection is not dynamic, i.e. the variables are injected
        // before the action listener code is called.
        String elementType = pathElement.getType();
        DocumentModel currentDoc;
        if (elementType == DocumentPathElement.TYPE) {
            DocumentPathElement docPathElement = (DocumentPathElement) pathElement;
            currentDoc = docPathElement.getDocumentModel();
            return navigationContext.navigateToDocument(currentDoc);
        } else if (elementType == ArchivedVersionsPathElement.TYPE) {
            ArchivedVersionsPathElement docPathElement = (ArchivedVersionsPathElement) pathElement;
            currentDoc = docPathElement.getDocumentModel();
            return navigationContext.navigateToDocument(currentDoc,
                    "TAB_CONTENT_HISTORY");
        } else if (elementType == VersionDocumentPathElement.TYPE) {
            VersionDocumentPathElement element = (VersionDocumentPathElement) pathElement;
            currentDoc = element.getDocumentModel();
            return navigationContext.navigateToDocument(currentDoc);
        }
        return null;
    }

    /**
     * Computes the current path by making calls to backend. TODO: need to
     * change to compute the path from the seam context state.
     * <p>
     * GR: removed the Factory annotation because it made the method be called
     * too early in case of processing that involves changing the current
     * document. Multiple invocation of this method is anyway very cheap.
     *
     * @return
     */
    @Factory(value = "backendPath", scope = EVENT)
    public List<PathElement> getBackendPath() throws ClientException {
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        String viewIdLabel = Pages.instance().getPage(viewId).getDescription();
        if (viewIdLabel != null && viewIdLabel.startsWith(BREADCRUMB_PREFIX)) {
            return makeBackendPathFromLabel(viewIdLabel.substring(BREADCRUMB_PREFIX.length() + 1));
        } else {
            return shrinkPathIfNeeded(navigationContext.getCurrentPathList());
        }
    }

    protected List<PathElement> shrinkPathIfNeeded(List<PathElement> paths) {

        if (paths == null || paths.size() <= MIN_PATH_SEGMENTS_LEN) {
            return paths;
        }

        StringBuffer sb = new StringBuffer();
        for (PathElement pe : paths) {
            sb.append(pe.getName());
        }
        String completePath = sb.toString();

        if (completePath.length() <= MAX_PATH_CHAR_LEN) {
            return paths;
        }

        // shrink path
        sb = new StringBuffer();
        List<PathElement> shrinkedPath = new ArrayList<PathElement>();
        for (int i = paths.size() - 1; i >= 0; i--) {
            PathElement pe = paths.get(i);
            sb.append(pe.getName());
            if (sb.length() < MAX_PATH_CHAR_LEN) {
                shrinkedPath.add(0, pe);
            } else {
                break;
            }
        }
        // be sure we have at least one item in the breadcrumb otherwise the upnavigation will fail
        if (shrinkedPath.size()==0) {
            // this means the current document has a title longer than MAX_PATH_CHAR_LEN !
            shrinkedPath.add(0, paths.get(paths.size()-1));
        }
        shrinkedPath.add(0, new TextPathElement(PATH_SHORTCUT));
        return shrinkedPath;
    }

    private List<PathElement> makeBackendPathFromLabel(String label) {
        List<PathElement> pathElements = new ArrayList<PathElement>();
        label = resourcesAccessor.getMessages().get(label);
        PathElement pathLabel = new TextPathElement(label);
        // add the label of the viewId to the path
        pathElements.add(pathLabel);
        return pathElements;
    }

}
