package org.nuxeo.dam.webapp.contentbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;

@Name("documentActions")
@Scope(ScopeType.CONVERSATION)
public class DocumentActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentActions.class);

    @In(create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    /**
     * Current selected asset
     */
    protected DocumentModel currentSelection;

    /**
     * Current selection link - defines the fragment to be shown under the tabs
     * list
     */
    protected String currentSelectionLink;

    protected String displayMode = "view";

    @WebRemote
    public String processSelectRow(String docRef, String providerName,
            String listName, Boolean selection) {
        PagedDocumentsProvider provider;
        try {
            provider = resultsProvidersCache.get(providerName);
        } catch (ClientException e) {
            return handleError(e.getMessage());
        }
        DocumentModel doc = null;
        for (DocumentModel pagedDoc : provider.getCurrentPage()) {
            if (pagedDoc.getRef().toString().equals(docRef)) {
                doc = pagedDoc;
                break;
            }
        }
        if (doc == null) {
            return handleError(String.format(
                    "could not find doc '%s' in the current page of provider '%s'",
                    docRef, providerName));
        }
        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        if (selection) {
            documentsListsManager.addToWorkingList(lName, doc);
        } else {
            documentsListsManager.removeFromWorkingList(lName, doc);
        }
        return computeSelectionActions(lName);
    }

    @WebRemote
    public String processSelectPage(String providerName, String listName,
            Boolean selection) {
        PagedDocumentsProvider provider;
        try {
            provider = resultsProvidersCache.get(providerName);
        } catch (ClientException e) {
            return handleError(e.getMessage());
        }
        DocumentModelList documents = provider.getCurrentPage();
        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        if (selection) {
            documentsListsManager.addToWorkingList(lName, documents);
        } else {
            documentsListsManager.removeFromWorkingList(lName, documents);
        }
        return computeSelectionActions(lName);
    }

    private String handleError(String errorMessage) {
        log.error(errorMessage);
        return "ERROR: " + errorMessage;
    }

    private String computeSelectionActions(String listName) {
        List<Action> availableActions = webActions.getUnfiltredActionsList(listName
                + "_LIST");
        List<String> availableActionIds = new ArrayList<String>();
        for (Action a : availableActions) {
            if (a.getAvailable()) {
                availableActionIds.add(a.getId());
            }
        }
        String res = "";
        if (!availableActionIds.isEmpty()) {
            res = StringUtils.join(availableActionIds.toArray(), "|");
        }
        return res;
    }

    public DocumentModel getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(DocumentModel selection) {
        // Reset the tabs list and the display mode
        webActions.resetTabList();
        displayMode = "view";

        currentSelection = selection;

        // Set first tab as current tab
        List<Action> tabList = webActions.getTabsList();
        if (tabList != null && tabList.size() > 0) {
            Action currentAction = tabList.get(0);
            webActions.setCurrentTabAction(currentAction);
            currentSelectionLink = currentAction.getLink();
        }
    }

    public String getCurrentSelectionLink() {
        if (currentSelectionLink == null) {
            return "/incl/tabs/empty_tab.xhtml";
        }
        return currentSelectionLink;
    }

    public void setCurrentTabAction(Action currentTabAction) {
        webActions.setCurrentTabAction(currentTabAction);
        currentSelectionLink = currentTabAction.getLink();
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void toggleDisplayMode() {
        if ("view".equals(displayMode)) {
            displayMode = "edit";
        } else {
            displayMode = "view";
        }
    }

    public String getPreviewURL() {
        if (currentSelection == null) {
            return null;
        }
        return PreviewHelper.getPreviewURL(currentSelection, null);
    }

    public void updateCurrentSelection() throws ClientException {
        if (currentSelection != null) {
            documentManager.saveDocument(currentSelection);
            documentManager.save();

            // Switch to view mode
            displayMode = "view";
        }
    }

    /**
     * Logs the user out. Invalidates the HTTP session so that it cannot be used
     * anymore.
     *
     * @return the next page that is going to be displayed
     * @throws IOException
     */
    public static String logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        Object req = eContext.getRequest();
        Object resp = eContext.getResponse();
        HttpServletRequest request = null;
        if (req instanceof HttpServletRequest) {
            request = (HttpServletRequest) req;
        }
        HttpServletResponse response = null;
        if (resp instanceof HttpServletResponse) {
            response = (HttpServletResponse) resp;
        }

        if (response != null && request != null
                && !context.getResponseComplete()) {
            String baseURL = BaseURL.getBaseURL(request);
            request.setAttribute(URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY,
                    true);
            response.sendRedirect(baseURL + NXAuthConstants.LOGOUT_PAGE);
            context.responseComplete();
        }
        return null;
    }
  
    /**
     * Takes in a DocumentModel, gets the 'title' from it, and crops
     * it to a maximum of 20 characters. If the Title is more than 20
     * it will return the Beginning of the title (first 9 characters), followed by 3 ellipses (...) 
     * followed by the End of the title (last 8 characters).
     *
     * @param DocumentModel document to extract the title from
     * @return String with the cropped title restricted to maximum of 20 characters
     */
    public String getTitleCropped(DocumentModel document) {
        
    	String title = null;
    	String firstNineChars = null;
    	String lastEightChars = null;
    	
    	log.info("cropping title to 20 characters");
    	try {
    		title = document.getTitle();
    	} catch (ClientException e) {
    		log.error("Exception while trying to retrieve 'title' of document. Returning a blank title.");
    	}
    	if(title == null) {
    		return "";
    	}
    	int length = title.length();
    	if(length <= 20) {
    		return title;
    	}
    	// length is more than 20 characters: use first 9 characters, plus three ellipses ..., plus last 8 characters
    	// to construct the new title
    	
    	// get the first 9 characters:
    	firstNineChars = title.substring(0, 9);
    	// get the last 8 characters:
    	lastEightChars = title.substring(length-8, length);
    	
    	return firstNineChars + "..." + lastEightChars;
    	
    }
    
    /**
     * Takes in a DocumentModel, gets the 'title' from it, and crops
     * it to a maximum of maxLength characters. If the Title is more than maxLength characters
     * it will return the Beginning of the title, followed by 3 ellipses (...) 
     * followed by the End of the title.
     * 
     * A minimum of 6 characters is needed before cropping takes effect.
     * If you specify a maxLength of less than 5, it is ignored - in this case maxLength will be set to begin at 5.
     *
     * @param DocumentModel document to extract the title from
     * @param int maxLength the maximum length of the title before cropping will occur
     * @return String with the cropped title restricted to maximum of maxLength characters
     */
    public String getTitleCropped(DocumentModel document, int maxLength) {
        
    	String title = null;
    	String beginningChars = null;
    	int nbrBeginningChars = -1;
    	String endChars = null;
    	int nbrEndChars = -1;
    	int nbrEllipses = 3;
    	int minLength = 5;
    	
    	log.debug("Cropping title to " + maxLength + " characters.");
    	try {
    		title = document.getTitle();
    	} catch (ClientException e) {
    		log.error("Exception while trying to retrieve 'title' of document. Returning a blank title.");
    	}
    	if(title == null) {
    		return "";
    	}
    	int length = title.length();
    	
    	// a minimum of 5 characters needed before we crop
    	if(length <= minLength) {
    		log.debug("Title is " + length + " characters. A minimum of 5 characters needed before we crop. Returning title unchanged.");
    		return title;
    	}
    	
    	// if maxLength is crazy, set it to a proper value
    	if(maxLength <= minLength ) {
    		log.debug("A maxLength of " + maxLength + " is unreasonable. Setting maxLength to " + minLength);
    		maxLength = minLength;
    	}
    	
    	if(length <= maxLength) {
    		log.debug("Title length " + length + " is less than maxLength " + maxLength + ". Returning title unchanged.");
    		return title;
    	}

    	// at this point we should be ok to start cropping to our heart's content
    	// length is more than maxLength characters: construct the new title
    	
    	// get the first (maxLength-3)/2 characters:
    	if((maxLength-nbrEllipses)%2==0) {
    		nbrBeginningChars = (maxLength-nbrEllipses)/2;
    	} else {
    		nbrBeginningChars = (maxLength-nbrEllipses)/2 + 1;
    	}
    	
    	beginningChars = title.substring(0, nbrBeginningChars);
    	// get the last n characters:
    	nbrEndChars = maxLength - nbrBeginningChars - nbrEllipses;
    	endChars = title.substring(length-nbrEndChars, length);
    	
    	String croppedTitle = beginningChars + "..." + endChars;
    	log.debug("Original title: [" + title + "]");
    	log.debug("Cropped title: [" + croppedTitle + "]");
    	return croppedTitle;
    	
    }

}
