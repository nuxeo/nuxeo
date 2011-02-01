  <div id="waitMessage">
       <img src="${clientSideBaseUrl}img/standart_waiter.gif">
  </div>

  <div id="oAuthPromptMessage" style="display: none">
     <table>
     <tr><td rowspan="2"><img src="${clientSideBaseUrl}site/skin/gadgets/img/oauth.png"></td>
     <td colspan="2">__MSG_label.nuxeo.oauth.login__</td></tr>
     <tr>
     <td><img src="${clientSideBaseUrl}site/skin/gadgets/img/nuxeo_logo.png"></td>
     <td><input id="nxauth" type="button" value="__MSG_action.nuxeo.oauth.login__"/></td>
     </table>

  </div>

  <div id="oAuthWaitMessage" style="display: none">
      __MSG_label.nuxeo.oauth.wait__
    <a href="#" id="approvaldone">__MSG_action.nuxeo.oauth.refresh__</a>
  </div>

  <div id="errorMessage">
  </div>