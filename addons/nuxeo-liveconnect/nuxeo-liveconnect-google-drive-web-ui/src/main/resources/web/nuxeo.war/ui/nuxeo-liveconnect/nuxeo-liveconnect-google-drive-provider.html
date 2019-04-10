<!--
(C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.

icensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
  Gabriel Barata <gbarata@nuxeo.com>
-->
<link rel="import" href="nuxeo-liveconnect-behavior.html">
<script src="https://apis.google.com/js/client.js" type="text/javascript"></script>

<!--
`nuxeo-liveconnect-google-drive-provider`
@group Nuxeo UI
@element nuxeo-liveconnect-google-drive-provider
-->
<dom-module id="nuxeo-liveconnect-google-drive-provider">

  <template>
    <style>
      :host {
        display: none;
      }
    </style>

    <nuxeo-resource id="oauth2"></nuxeo-resource>
  </template>

  <script>
    Polymer({
      is: 'nuxeo-liveconnect-google-drive-provider',

      behaviors: [Nuxeo.LiveConnectBehavior],

      properties: {
        providerId: {
          value: 'googledrive'
        }
      },

      openPicker: function() {
        gapi.load('picker', {
          'callback': this._init.bind(this)
        });
      },

      _init: function() {
        this.updateProviderInfo().then(function() {
          if (!this.isUserAuthorized) {
            this.openPopup(this.authorizationURL, {
              onMessageReceive: this._parseMessage.bind(this),
              onClose: this._onOAuthPopupClose.bind(this)
            });
          } else {
            this._doAuth(true, this._checkAuth.bind(this));
          }
        }.bind(this));
      },

      _doAuth: function(immediate, callback) {
        var obj = {
          client_id: this.clientId,
          scope: 'email https://www.googleapis.com/auth/drive'
        };
        if (this.userId) {
          obj.user_id = this.userId;
          obj.immediate = immediate;
        } else {
          obj.authuser = -1;
        }
        if (this.domain) {
          obj.hd = this.domain;
        }
        gapi.auth.authorize(obj, callback);
      },

      _checkAuth: function() {
        var token = gapi.auth.getToken();
        if (token) {
          this._handleAuthResult(token.access_token);
        } else {
          this._doAuth(false, this._checkAuth.bind(this));
        }
      },

      _parseMessage: function(event) {
        var data = JSON.parse(event.data);
        this.accessToken = data.token;
      },

      _onOAuthPopupClose: function() {
        if (this.accessToken) {
          this._handleAuthResult(this.accessToken);
        }
      },

      _handleAuthResult: function(token) {
        if (token) {
          var xhr = document.createElement('iron-request');
          xhr.send({url:'https://www.googleapis.com/oauth2/v1/tokeninfo?access_token= ' + token, handleAs: 'json'})
              .then(function() {
                this.userId = xhr.response.email;
                this._showPicker(token);
              }.bind(this));
        } else {
          this._checkAuth(false);
        }
      },

      _showPicker: function(accessToken) {
        var view = new google.picker.DocsView();
        view.setIncludeFolders(true);
        view.setOwnedByMe(true);
        new google.picker.PickerBuilder()
            .setOAuthToken(accessToken)
            .setAppId(this.clientId)
            .addView(view)
            .setCallback(this._pickerCallback.bind(this))
            .enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
            .build()
            .setVisible(true);
      },

      _pickerCallback: function(data) {
        var action = data[google.picker.Response.ACTION];
        if (action === google.picker.Action.PICKED) {
          var files = [];
          data[google.picker.Response.DOCUMENTS].forEach(function(file) {
            files.push({
              providerId: this.providerId,
              providerName: 'Google Drive',
              user: this.userId,
              fileId: file.id,
              name: file.name,
              size: file.sizeBytes,
              key: this.generateBlobKey(file.id)
            });
          }.bind(this));
          this.notifyBlobPick(files);
        }
      }
    });
  </script>
</dom-module>
