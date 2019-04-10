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
<script src="https://app.box.com/js/static/select.js" type="text/javascript"></script>

<!--
`nuxeo-liveconnect-box-provider`
@group Nuxeo UI
@element nuxeo-liveconnect-box-provider
-->
<dom-module id="nuxeo-liveconnect-box-provider">

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
      is: 'nuxeo-liveconnect-box-provider',

      behaviors: [Nuxeo.LiveConnectBehavior],

      properties: {
        providerId: {
          value: 'box'
        }
      },

      openPicker: function() {
        this.updateProviderInfo().then(this._init.bind(this));
      },

      _init: function() {
        if (!this.isUserAuthorized) {
          this.openPopup(this.authorizationURL, {
            onMessageReceive: this._parseMessage.bind(this),
            onClose: this._onOAuthPopupClose.bind(this)
          });
        } else {
          this._showPicker();
        }
      },

      _parseMessage: function(event) {
        var data = JSON.parse(event.data);
        this.accessToken = data.token;
      },

      _onOAuthPopupClose: function() {
        if (this.accessToken) {
          if (!this.userId) {
            this.updateProviderInfo().then(function() {
              if (!this.userId) {
                throw 'No username available.';
              }
              this._showPicker();
            }.bind(this));
          } else {
            this._showPicker();
          }
        }
      },

      _showPicker: function() {
        var options = {
              clientId: this.clientId,
              linkType: 'direct',
              multiselect: true
            };
        var boxSelect = new BoxSelect(options);

        boxSelect.success(function(response) {
          var blobs = [];
          response.forEach(function(file) {
            blobs.push({
              providerId: this.providerId,
              providerName: 'Box',
              user: this.userId,
              fileId: file.id.toString(),
              name: file.name,
              size: file.size,
              key: this.generateBlobKey(file.id)
            });
          }.bind(this));
          this.notifyBlobPick(blobs);
        }.bind(this));

        // open picker
        boxSelect.launchPopup();
      }

    });
  </script>
</dom-module>
