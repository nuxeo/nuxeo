var nuxeo = nuxeo || {};

nuxeo.utils = (function(m) {

    // picks a doc from Google Drive
    // triggered by a button
    // receives input element id (to save doc id)
    // and info element id (to display info about the doc)
    m.pickFromGoogleDrive = function(clientId, inputId, infoId) {
        var picker = new nuxeo.utils.GoogleDrivePicker({
            clientId : clientId,
            onDocumentSelected : function(doc) {
                if (inputId) {
                    document.getElementById(inputId).value = doc[google.picker.Document.ID];
                }
                if (infoId) {
                    document.getElementById(infoId).innerHTML = '<img src="' + doc[google.picker.Document.ICON_URL]
                            + '"/> ' + doc[google.picker.Document.NAME];
                }
            }
        });
    };

    m.GoogleDrivePicker = function(options) {
        this.clientId = options.clientId;
        this.onDocumentSelected = options.onDocumentSelected;
        gapi.load('picker', {
            'callback' : this._open.bind(this)
        });
    };

    m.GoogleDrivePicker.prototype = {

        _open : function() {
            this._doAuth(true, this._checkAuth.bind(this));
        },

        _checkAuth : function() {
            var token = gapi.auth.getToken();
            if (token) {
                this._showPicker();
            } else {
                this._doAuth(false, this._showPicker.bind(this));
            }
        },

        _doAuth : function(immediate, callback) {
            gapi.auth.authorize({
                client_id : this.clientId,
                scope : 'https://www.googleapis.com/auth/drive.readonly',
                immediate : immediate
            }, callback);
        },

        _showPicker : function() {
            var view = new google.picker.DocsView();
            view.setIncludeFolders(true);
            this.picker = new google.picker.PickerBuilder() //
            // .enableFeature(google.picker.Feature.MINE_ONLY) //
            .setOAuthToken(gapi.auth.getToken().access_token) //
            .setAppId(this.clientId) //
            .addView(view) //
            .setCallback(this._pickerCallback.bind(this)) //
            .build() //
            .setVisible(true);
        },

        _pickerCallback : function(data) {
            var action = data[google.picker.Response.ACTION];
            if (action == google.picker.Action.PICKED) {
                var doc = data[google.picker.Response.DOCUMENTS][0];
                this.onDocumentSelected(doc);
            }
        }

    };

    return m

}(nuxeo.utils || {}));
