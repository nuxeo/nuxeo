var nuxeo = nuxeo || {};
nuxeo.utils = nuxeo.utils || {};

// picks a doc from Box
// parameters:
// clientId: the OAuth client id to use
// inputId: element id of input fields to save the doc path
// infoId: element id of span to fill with doc info
// token: access token for current user (may be null)
// authorizationUrl: OAuth flow url
// baseUrl: The base url of REST API
nuxeo.utils.OneDrivePicker = function(clientId, inputId, infoId, token, authorizationUrl, baseUrl) {
  this.clientId = clientId;
  this.inputId = inputId;
  this.infoId = infoId;
  this.token = token;
  this.authorizationUrl = authorizationUrl;
  this.baseUrl = baseUrl;

  this.init();

};

nuxeo.utils.OneDrivePicker.prototype = {

  init: function() {
    if (this.token !== "" || nuxeo.utils.OneDrivePicker.ignoreOAuthPopup == true) {
      this.showPicker.call(this);
    } else {
      openPopup(this.authorizationUrl, {
        onMessageReceive: this.parseMessage.bind(this),
        onClose: this.onOAuthPopupClose.bind(this)
      });
    }
  },

  onOAuthPopupClose : function() {
    if (this.token) {
      this.showPicker.call(this);
      nuxeo.utils.OneDrivePicker.ignoreOAuthPopup = true;
    }
  },

  parseMessage: function(event) {
    var data = JSON.parse(event.data);
    this.token = data.token;
  },

  showPicker: function () {
    var options = {
      baseURL: this.baseUrl,
      accessToken: this.token
    };
    var filePicker = new OneDriveFilePicker(options);

    // open picker and handle result
    filePicker.select().then(function(result) {
      if (result.action === 'select') {
        if (this.inputId) {
          document.getElementById(this.inputId).value = result.item.id;
        }
        if (this.infoId) {
          document.getElementById(this.infoId).innerHTML = result.item.name;
        }
      }
    }.bind(this));
  }

};
