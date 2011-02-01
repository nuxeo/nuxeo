var currentPage = 0;
var maxPage = 0;

      function showErrorMessage(message) {
        _gel("errorMessage").innerHTML=message;
        _gel("errorMessage").style.display='block';
      }
      function hideErrorMessage(message) {
        _gel("errorMessage").innerHTML="";
        _gel("errorMessage").style.display='none';
      }
      function showWaitMessage() {
        _gel("waitMessage").style.display='block';
      }
      function hideWaitMessage() {
        _gel("waitMessage").style.display='none';
      }
      function showOAuthPrompt(openCallback, doneCallback) {
        _gel("oAuthPromptMessage").style.display='block';
        _gel('nxauth').onclick = openCallback;
        _gel('approvaldone').onclick = doneCallback;
        hideWaitMessage();
      }
      function hideOAuthPrompt(openCallback, doneCallback) {
          _gel("oAuthPromptMessage").style.display='none';
      }
      function showOAuthInProgress() {
        _gel("oAuthWaitMessage").style.display='block';
        hideOAuthPrompt();
      }
      function hideOAuthInProgress() {
        _gel("oAuthWaitMessage").style.display='none';
      }

      function doAutomationRequest(nxParams) {

        showWaitMessage();
        hideErrorMessage();

        var ts = new Date().getTime() + "" + Math.random()*11
        var url = NXGadgetContext.serverSideBaseUrl + "site/automation/" + nxParams.operationId;
        url += "?ts=" + ts;

        var rParams = {};
        if (NXGadgetContext.insideNuxeo) {
          rParams[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.SIGNED;
          rParams[gadgets.io.RequestParameters.OAUTH_SERVICE_NAME] = "nuxeo4shindig";
        } else {
          rParams[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.OAUTH;
          rParams[gadgets.io.RequestParameters.OAUTH_SERVICE_NAME] = "nuxeo";
          rParams[gadgets.io.RequestParameters.OAUTH_USE_TOKEN]="always";
        }
        rParams[gadgets.io.RequestParameters.METHOD] = gadgets.io.MethodType.POST;

        rParams[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;

        if (nxParams.usePagination) {
          nxParams.operationParams.page=currentPage;
        }

        // Build automation body
        var automationParams = {};
        automationParams.input=nxParams.operationInput;
        automationParams.params=nxParams.operationParams;
        automationParams.context=nxParams.operationContext;
        automationParams.documentProperties=nxParams.operationDocumentProperties;

        rParams[gadgets.io.RequestParameters.POST_DATA] = gadgets.io.encodeValues({jsondata : JSON.stringify(automationParams)});

        gadgets.io.makeRequest(url, function(response) {requestCompleted(response,nxParams);}, rParams);
    }

    function requestCompleted(response,nxParams) {
      if (response.data) {
        hideWaitMessage();
        hideOAuthInProgress();
        if (response.data['entity-type']==nxParams.entityType) {
          if (nxParams.usePagination) {
            maxPage = response.data['pageCount'];
          }
          nxParams.displayMethod(response.data.entries,nxParams);
        }
        else {
          alert(response.data.entity-type);
        }
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
      }
      else {
        showErrorMessage("No data received from server");
      }
    }

