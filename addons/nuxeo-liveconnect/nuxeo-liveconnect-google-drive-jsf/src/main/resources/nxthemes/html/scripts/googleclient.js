var nuxeo = nuxeo || {};
nuxeo.utils = nuxeo.utils || {};

// picks a doc from Google Drive
// parameters:
// clientId: the OAuth client id to use
// pickId: element id of "pick from google drive" span
// authId: element id of "please authenticate" span, initially hidden
// inputId: element id of input fields to save the doc id
// infoId: element id of span to fill with doc info
// domain: limit account picker to this domain
// authorizationUrl: OAuth flow url
nuxeo.utils.GoogleDrivePicker = function(clientId, pickId, authId, inputId, infoId, domain, authorizationUrl) {
    this.clientId = clientId;
    this.pickId = pickId;
    this.authId = authId;
    this.inputId = inputId;
    this.infoId = infoId;
    this.domain = domain;
    this.url = authorizationUrl;

    if (window.gapi) {
      this.load();
    } else {
      var script = document.createElement("script");
      script.type = "text/javascript";
      script.src = "https://apis.google.com/js/client.js";
      script.onload = this.load.bind(this);
      document.head.appendChild(script);
    }
};

nuxeo.utils.GoogleDrivePicker.prototype = {

    load: function() {
      gapi.load('picker', {
        'callback' : this.init.bind(this)
      });
    },

    init : function() {
        // try immediate first. if it fails, ask user to re-click the button
        var immediate = !this.isAskingForAuth();
        this.doAuth(immediate, this.checkAuth.bind(this));
    },

    isAskingForAuth : function() {
        return document.getElementById(this.authId).offsetWidth > 0;
    },

    checkAuth : function() {
        if (!(this.url == "" || nuxeo.utils.GoogleDrivePicker.ignoreOAuthPopup == true)) {
          openPopup(this.url, {
            onMessageReceive: this.parseMessage.bind(this),
            onClose: this.onOAuthPopupClose.bind(this)
          });
        } else {
          var token = gapi.auth.getToken();
          if (token) {
            this.onAuth(token.access_token);
          } else {
            this.doAuth(false, this.checkAuth.bind(this));
          }
        }
    },

    doAuth : function(immediate, callback) {
        gapi.auth.authorize({
            client_id : this.clientId,
            scope : 'email https://www.googleapis.com/auth/drive.readonly',
            immediate : immediate,
            hd : this.domain
        }, callback);
    },

    onAuth : function(accessToken) {
      // retrieve the account's email
      jQuery.getJSON("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token= " + accessToken)
          .done(function(info) {
            this.email = info.email;
            // display the picker
            this.showPicker(accessToken);
          }.bind(this));
    },

    showPicker : function(accessToken) {
        var view = new google.picker.DocsView();
        view.setIncludeFolders(true);
        view.setOwnedByMe(true);
        new google.picker.PickerBuilder() //
        .setOAuthToken(accessToken) //
        .setAppId(this.clientId) //
        .addView(view) //
        .setCallback(this.pickerCallback.bind(this)) //
        .build() //
        .setVisible(true);
    },

    pickerCallback : function(data) {
        var action = data[google.picker.Response.ACTION];
        if (action == google.picker.Action.PICKED) {
            var doc = data[google.picker.Response.DOCUMENTS][0];
            if (this.inputId) {
                document.getElementById(this.inputId).value = this.email + ':' + doc[google.picker.Document.ID];
            }
            if (this.infoId) {
                document.getElementById(this.infoId).innerHTML = '<img src="' + doc[google.picker.Document.ICON_URL]
                        + '"/> ' + doc[google.picker.Document.NAME];
            }
        }
    },

    parseMessage: function(event) {
      var data = JSON.parse(event.data);
      this.accessToken = data.token;
    },

    onOAuthPopupClose : function() {
      if (this.accessToken) {
        this.onAuth(this.accessToken);
        nuxeo.utils.GoogleDrivePicker.ignoreOAuthPopup = true;
      }
    }

};
