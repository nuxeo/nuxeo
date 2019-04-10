<!--
(C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and contributors.
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
  Thomas Roger <troger@nuxeo.com>
-->

<!--
`nuxeo-wopi-link`
@group Nuxeo UI
@element nuxeo-wopi-link
-->
<dom-module id="nuxeo-wopi-link">
  <template>
    <template is="dom-if" if="[[_isAvailable(url)]]">
      <nuxeo-link-button href="[[url]]" icon-src="[[_wopiIcon(appName)]]" label="[[_wopiTooltip(appName)]]" target="_blank"></nuxeo-link-button>
    </template>
  </template>

  <script>
    Polymer({
      is: 'nuxeo-wopi-link',

      behaviors: [Nuxeo.FiltersBehavior],

      properties: {
        document: Object,
        blob: Object,
        appName: {
          type: String,
          computed: '_appName(blob)'
        },
        url: {
          type: String,
          computed: '_wopiURL(document, blob)'
        }
      },

      _appName() {
        return this.blob
          && this.blob.wopi
          && this.blob.wopi.appName
          && this.blob.wopi.appName.toLowerCase();
      },

      _isAvailable() {
        return !!this.url;
      },

      _wopiIcon() {
        return 'images/' + this.appName + '.png'
      },

      _wopiTooltip() {
        return 'wopiLink.' + this.appName + '.tooltip';
      },

      _wopiURL() {
        var blobInfo = this.blob && this.blob.wopi;
        if (!blobInfo) {
          return null;
        }

        return blobInfo.edit && this.hasPermission(this.document, 'WriteProperties') ? blobInfo.edit : blobInfo.view;
      }
    });
  </script>

</dom-module>
