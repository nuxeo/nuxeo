var nuxeo = nuxeo || {};
nuxeo.utils = nuxeo.utils || {};

// picks a doc from Dropbox
// parameters:
// inputId: element id of input fields to save the doc path
// infoId: element id of span to fill with doc info
// authorizationUrl: OAuth flow url
nuxeo.utils.DropboxPicker = function(inputId, infoId, url, clientId) {
  this.inputId = inputId;
  this.infoId = infoId;
  this.url = url;

  if (window.Dropbox) {
    this.init();
  } else {
    var script = document.createElement("script");
    script.type = "text/javascript";
    script.id = "dropboxjs";
    script.setAttribute("data-app-key", clientId);
    script.src = "https://www.dropbox.com/static/api/2/dropins.js";
    script.onload = this.init.bind(this);
    document.head.appendChild(script);
  }
};

nuxeo.utils.DropboxPicker.prototype = {

  init: function() {
    if (this.url == "" || nuxeo.utils.DropboxPicker.ignoreOAuthPopup == true) {
      this.showPicker.call(this);
    } else {
      openPopup(this.url, {
        onMessageReceive: this.parseMessage.bind(this),
        onClose: this.onOAuthPopupClose.bind(this)
      });
    }
  },

  onOAuthPopupClose : function() {
    if (this.token) {
      this.showPicker.call(this);
      nuxeo.utils.DropboxPicker.ignoreOAuthPopup = true;
    }
  },

  parseMessage: function(event) {
    var data = JSON.parse(event.data);
    this.token = data.token;
  },

  showPicker: function () {
    var options = {
      success: function(files) {
        var doc = files[0];
        if (this.inputId) {
          document.getElementById(this.inputId).value = doc.link ;
        }
        if (this.infoId) {
          document.getElementById(this.infoId).innerHTML = '<img width="16" height="16" src="' + doc.icon
              + '"/> ' + doc.name;
        }
      }.bind(this),

      // "preview" is a preview link to the document for sharing,
      // "direct" is an expiring link to download the contents of the file.
      linkType: "direct"
    };

    // open picker
    Dropbox.choose(options);
  }

};