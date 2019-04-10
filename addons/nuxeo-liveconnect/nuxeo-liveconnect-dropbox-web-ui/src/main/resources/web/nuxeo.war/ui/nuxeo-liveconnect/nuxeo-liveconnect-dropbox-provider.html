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

<!--
`nuxeo-liveconnect-dropbox-provider`
@group Nuxeo UI
@element nuxeo-liveconnect-dropbox-provider
-->
<dom-module id="nuxeo-liveconnect-dropbox-provider">

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
      is: 'nuxeo-liveconnect-dropbox-provider',

      behaviors: [Nuxeo.LiveConnectBehavior],

      properties: {
        providerId: {
          value: 'dropbox'
        }
      },

      openPicker: function() {
        this.updateProviderInfo().then(this._init.bind(this));
      },

      _init: function() {
        var self = this;
        function auth() {
          if (!self.isUserAuthorized) {
            self.openPopup(self.authorizationURL, {
              onMessageReceive: self._parseMessage.bind(self),
              onClose: self._onOAuthPopupClose.bind(self)
            });
          } else {
            self._showPicker();
          }
        }
        if (window.Dropbox) {
          auth();
        } else {
          var script = document.createElement('script');
          script.type = 'text/javascript';
          script.id = 'dropboxjs';
          script.setAttribute('data-app-key', this.clientId);
          script.src = 'https://www.dropbox.com/static/api/2/dropins.js';
          script.onload = auth.bind(this);
          document.head.appendChild(script);
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
          success: function(files) {
            var blobs = [];
            files.forEach(function(file) {
              var fileId = this._getPathFromUrl(file.link); // NXP-22530: Replace path with dropbox file id
              blobs.push({
                providerId: this.providerId,
                providerName: 'Dropbox',
                user: this.userId,
                fileId: fileId,
                name: file.name,
                size: file.bytes,
                key: this.generateBlobKey(fileId)
              });
            }.bind(this));
            this.notifyBlobPick(blobs);
          }.bind(this),

          cancel: function() {
          },

          linkType: 'direct',

          multiselect: true
        };

        Dropbox.choose(options);
      },

      _getPathFromUrl: function(url) {
        var path = url.replace(/https:\/\/dl.dropboxusercontent.com\/1\/view\/[\w]*/g, '');
        return decodeURIComponent(path);
      }

    });
  </script>
</dom-module>
