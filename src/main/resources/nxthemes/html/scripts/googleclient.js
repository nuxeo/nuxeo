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
nuxeo.utils.GoogleDrivePicker = function(clientId, pickId, authId, inputId, infoId, domain) {
    this.clientId = clientId;
    this.pickId = pickId;
    this.authId = authId;
    this.inputId = inputId;
    this.infoId = infoId;
    this.domain = domain;
    gapi.load('picker', {
        'callback' : this.init.bind(this)
    });
};

nuxeo.utils.GoogleDrivePicker.prototype = {

    init : function() {
        // try immediate first. if it fails, ask user to re-click the button
        var immediate = !this.isAskingForAuth();
        this.doAuth(immediate, this.checkAuth.bind(this));
    },

    isAskingForAuth : function() {
        return document.getElementById(this.authId).offsetWidth > 0;
    },

    checkAuth : function() {
        var token = gapi.auth.getToken();
        if (token) {
            if (this.isAskingForAuth()) {
                // re-display regular label
                document.getElementById(this.pickId).style.display = "";
                document.getElementById(this.authId).style.display = "none";
            }
            this.onAuth(token);
        } else {
            // ask the user for a second click,
            // this is needed to work with popup blockers
            document.getElementById(this.pickId).style.display = "none";
            document.getElementById(this.authId).style.display = "";
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

    onAuth : function(token) {
      // retrieve the account's email
      jQuery.getJSON("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token= " + token.access_token)
          .done(function(info) {
            this.email = info.email;
            // display the picker
            this.showPicker();
          }.bind(this));
    },

    showPicker : function() {
        var view = new google.picker.DocsView();
        view.setIncludeFolders(true);
        new google.picker.PickerBuilder() //
        // .enableFeature(google.picker.Feature.MINE_ONLY) //
        .setOAuthToken(gapi.auth.getToken().access_token) //
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
    }

};
