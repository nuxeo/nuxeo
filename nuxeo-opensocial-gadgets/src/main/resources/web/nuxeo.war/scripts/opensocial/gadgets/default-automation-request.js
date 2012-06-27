var currentPage = 0;
var maxPage = 0;
var unknownSize = false;

function showErrorMessage(message, debug) {
    _gel("errorMessage").innerHTML = message;
    if (debug) {
        _gel("debugInfo").innerHTML = debug;
    }
    _gel("errorDivMessage").style.display = 'block';
}
function hideErrorMessage() {
    _gel("errorMessage").innerHTML = "";
    _gel("debugInfo").innerHTML = "";
    _gel("errorDivMessage").style.display = 'none';
}
function showWaitMessage() {
    _gel("waitMessage").style.display = 'block';
}
function hideWaitMessage() {
    _gel("waitMessage").style.display = 'none';
}
function showOAuthPrompt(openCallback, doneCallback) {
    _gel("oAuthPromptMessage").style.display = 'block';
    _gel('nxauth').onclick = openCallback;
    _gel('approvaldone').onclick = doneCallback;
    hideWaitMessage();
}
function hideOAuthPrompt(openCallback, doneCallback) {
    _gel("oAuthPromptMessage").style.display = 'none';
}
function showOAuthInProgress() {
    _gel("oAuthWaitMessage").style.display = 'block';
    hideOAuthPrompt();
}
function hideOAuthInProgress() {
    _gel("oAuthWaitMessage").style.display = 'none';
}

function doAutomationRequest(nxParams) {

    showWaitMessage();
    hideErrorMessage();

    var url = NXGadgetContext.serverSideBaseUrl + "site/automation/" + nxParams.operationId;

    // add random TS to walkaround caching issues ...
    var ts = new Date().getTime() + "" + Math.random() * 11;
    url += "?ts=" + ts;

    // add target repository if needed
    if (typeof(getTargetRepository) == 'function') {
        var repoName = getTargetRepository();
        if (repoName != null && repoName != '') {
            url += "&nxrepository=" + repoName;
        }
    }

    var rParams = {};
    // select auth mode
    if (NXGadgetContext.insideNuxeo) {
        rParams[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.SIGNED;
        rParams[gadgets.io.RequestParameters.OAUTH_SERVICE_NAME] = "nuxeo4shindig";
    } else {
        rParams[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.OAUTH;
        rParams[gadgets.io.RequestParameters.OAUTH_SERVICE_NAME] = "nuxeo";
        rParams[gadgets.io.RequestParameters.OAUTH_USE_TOKEN] = "always";
    }
    rParams[gadgets.io.RequestParameters.METHOD] = gadgets.io.MethodType.POST;

    rParams[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

    if (nxParams.usePagination) {
        nxParams.operationParams.page = currentPage;
    }

    // Build automation body
    var automationParams = {};
    automationParams.input = nxParams.operationInput;
    automationParams.params = nxParams.operationParams;
    automationParams.context = nxParams.operationContext;
    automationParams.documentProperties = nxParams.operationDocumentProperties;

    rParams[gadgets.io.RequestParameters.POST_DATA] = gadgets.io.encodeValues(
            {jsondata : JSON.stringify(automationParams)});

    gadgets.io.makeRequest(url, function(response) {
        requestCompleted(response, nxParams);
    }, rParams);
}

function requestCompleted(response, nxParams) {
    hideWaitMessage();
    if (response.data) {
        hideOAuthInProgress();
        if (response.data['entity-type'] == "documents" ) { // old behavior for 'documents' output type
            if (nxParams.usePagination) {
                maxPage = response.data['pageCount'];
		if (response.data['totalSize'] < 0) {
		    unknownSize = true;
		    maxPage = currentPage + 2;
		}
            }
            // set callback
            nxParams.refreshCB = doAutomationRequest;
            nxParams.displayMethod(response.data.entries, nxParams);
        }
        // TODO handle the cases for other output types : document, blob, ...
        //else {
        //    alert(response.data['entity-type']);
        //}
    } else if (response.oauthApprovalUrl) {
        var onOpen = function() {
            showOAuthInProgress();
        };

        var onClose = function() {
            doAutomationRequest(nxParams);
        };
        var popup = new gadgets.oauth.Popup(response.oauthApprovalUrl,
                'height=600,width=800', onOpen, onClose);
        showOAuthPrompt(popup.createOpenerOnClick(), popup.createApprovedOnClick());
    } else if ( response.rc == 204 ) { // operation successful but not data
        hideOAuthInProgress();
    } else {
    	showErrorMessage("No data received from server: ", response.errors);
    }

    // call "operationCallback" method if defined
    // this will allow customized behavior when operation is finished
    if ( typeof(nxParams.operationCallback) != 'undefined' ) {
    	nxParams.operationCallback(response, nxParams)
    }


}

