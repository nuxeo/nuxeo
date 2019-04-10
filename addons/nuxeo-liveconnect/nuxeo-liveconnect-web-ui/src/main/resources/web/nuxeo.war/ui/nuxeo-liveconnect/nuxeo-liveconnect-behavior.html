<!--
(C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
Licensed under the Apache License, Version 2.0 (the 'License');
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
​
    http://www.apache.org/licenses/LICENSE-2.0
​
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
  Gabriel Barata <gbarata@nuxeo.com>
-->
<script>
var Nuxeo = Nuxeo || {};
Nuxeo.LiveConnectBehavior = {
  properties: {
    clientId: {
      type: String
    },
    authorizationURL: {
      type: String
    },
    providerId: {
      type: String
    },
    userId: {
      type: String
    },
    isUserAuthorized: {
      type: Boolean,
      value: false,
      notify: true
    },
    isAvailable: {
      type: Boolean,
      value: false,
      notify: true
    }
  },

  generateBlobKey: function(fileId) {
    if (!this.providerId) {
      throw 'failed to generate key: providerId not defined';
    }
    if (!this.userId) {
      throw 'failed to generate key: userId not defined';
    }
    if (!fileId) {
      throw 'failed to generate key: fileId not defined';
    }
    return this.providerId + ':' + this.userId + ':' + fileId;
  },

  notifyBlobPick: function(blobs) {
    this.fire('nx-blob-picked', {blobs: Array.isArray(blobs) ? blobs : [blobs]});
  },

  openPopup: function(url, options) {
    var popup, checkCompleted, settings, listener;

    settings = {
      'width': '1000',
      'height': '650',
      'onClose': function() {},
      'onMessageReceive': function() {}
    };

    if (options) {
      Object.assign(settings, options);
    }

    var left = window.screenX + (window.outerWidth / 2) - (settings.width / 2);
    var top = window.screenY + (window.outerHeight / 2) - (settings.height / 2);

    if (typeof settings.onMessageReceive === 'function') {
      listener = function(event) {
        settings.onMessageReceive(event);
      };
      window.addEventListener('message', listener);
    }

    popup = window.open(url, 'popup',
      'height=' + settings.height +
      ',width=' + settings.width +
      ',top=' + top +
      ',left=' + left);

    checkCompleted = setInterval(function() {
      if (!popup || !popup.closed) {
        return;
      }

      clearInterval(checkCompleted);
      if (typeof settings.onClose === 'function') {
        settings.onClose();
      }
      window.removeEventListener('message', listener);
    }, 100);
  },

  updateProviderInfo: function() {
    if (!this.$.oauth2) {
      throw 'Missing OAuth2 resource';
    }
    this.$.oauth2.path = 'oauth2/provider/' + this.providerId;
    return this.$.oauth2.get().then(function(response) {
      this.clientId = response.clientId;
      this.authorizationURL = response.authorizationURL;
      this.isUserAuthorized = response.isAuthorized;
      this.userId = response.userId;
      this.isAvailable = response.isAvailable;
    }.bind(this));
  },

  getToken: function() {
    if (!this.$.oauth2) {
      throw 'Missing OAuth2 resource';
    }
    this.$.oauth2.path = 'oauth2/provider/' + this.providerId + '/token';
    return this.$.oauth2.get();
  },

  // extension point
  openPicker: function() {
    throw 'not implemented';
  }
};
</script>
