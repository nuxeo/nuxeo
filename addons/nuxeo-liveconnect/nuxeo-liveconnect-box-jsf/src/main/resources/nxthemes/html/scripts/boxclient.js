var nuxeo = nuxeo || {};
nuxeo.utils = nuxeo.utils || {};

// picks a doc from Box
// parameters:
// clientId: the OAuth client id to use
// inputId: element id of input fields to save the doc path
// infoId: element id of span to fill with doc info
// authorizationUrl: OAuth flow url
nuxeo.utils.BoxPicker = function(clientId, inputId, infoId, authorizationUrl) {
  this.clientId = clientId;
  this.inputId = inputId;
  this.infoId = infoId;
  this.authorizationUrl = authorizationUrl;

  if (window.BoxSelect) {
    this.init();
  } else {
    var script = document.createElement("script");
    script.type = "text/javascript";
    script.id = "boxjs";
    script.src = "https://app.box.com/js/static/select.js";
    script.onload = this.init.bind(this);
    document.head.appendChild(script);
  }
};

nuxeo.utils.BoxPicker.prototype = {

  init: function() {
    if (this.authorizationUrl == "" || nuxeo.utils.BoxPicker.ignoreOAuthPopup == true) {
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
      nuxeo.utils.BoxPicker.ignoreOAuthPopup = true;
    }
  },

  parseMessage: function(event) {
    var data = JSON.parse(event.data);
    this.token = data.token;
  },

  showPicker: function () {
    var options = {
      clientId: this.clientId,
      // "shared" is a shared link to the document for sharing,
      // "direct" is an expiring link to download the contents of the file.
      linkType: "direct",
      multiselect: false
    };
    var boxSelect = new BoxSelect(options);

    boxSelect.success(function(response) {
      var file = response[0];
      if (this.inputId) {
        document.getElementById(this.inputId).value = file.id;
      }
      if (this.infoId) {
        document.getElementById(this.infoId).innerHTML = file.name;
      }
    }.bind(this));

    // open picker
    boxSelect.launchPopup();
  }

};
