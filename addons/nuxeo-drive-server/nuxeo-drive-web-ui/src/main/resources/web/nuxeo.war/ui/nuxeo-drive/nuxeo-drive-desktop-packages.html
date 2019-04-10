<!--
(C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
  Nelson Silva <nsilva@nuxeo.com>
-->

<!--
An element listing Nuxeo Drive desktop client packages for download.

Example:

    <nuxeo-drive-desktop-packages application="Nuxeo Drive"></nuxeo-drive-desktop-packages>

@group Nuxeo UI Elements
@element nuxeo-drive-desktop-packages
-->
<dom-module id="nuxeo-drive-desktop-packages">
  <template>
    <style include="iron-flex iron-flex-alignment iron-flex-factors nuxeo-styles">
      :host {
        display: block;
      }

      .table {
        font-family: var(--nuxeo-app-font);
        line-height: 3.5;
      }

      .row {
        border-bottom: 1px solid var(--nuxeo-border);
        @apply --layout-horizontal;
      }

      .row:hover {
        background-color: var(--nuxeo-container-hover);
      }

      .header {
        background-color: var(--nuxeo-table-header-background);
        color: var(--nuxeo-table-header-titles);
        font-weight: 400;
        height: 56px;
        display: flex;
        flex-direction: row;
      }

      .cell {
        padding: 0 24px 0 24px;
        min-height: 46px;
        overflow: hidden;
      }

      paper-button {
        line-height: normal;
      }

      .platform {
        background-color: #50c3f0;
        border-radius: 3px;
        color: #fff;
        font-size: .9em;
        letter-spacing: .04em;
        line-height: 130%;
        margin: 0 0.2em 0.2em 0;
        padding: 0.06em 0.3em;
        text-transform: uppercase;
        vertical-align: baseline;
        white-space: nowrap
      }
    </style>

    <nuxeo-connection platform-version="{{_tp}}"></nuxeo-connection>

    <div class="table">
      <div class="header">
        <div class="cell flex">[[i18n('driveDesktopPackages.platform', 'Platform')]]</div>
        <div class="cell flex-3">[[i18n('driveDesktopPackages.install', 'Package to Install')]]</div>
      </div>
      <template is="dom-repeat" items="[[packages]]" as="pkg">
        <div class="row">
          <div class="cell flex"><span class="platform">[[pkg.platform]]</span></div>
          <div class="cell flex-3">
            <a href$="[[pkg.url]]" tabindex="-1" target="_blank">
              <paper-button noink>
                [[pkg.name]]
              </paper-button>
            </a>
          </div>
        </div>
      </template>
    </div>
  </template>
  <script>
    (function() {

      Polymer({
        is: 'nuxeo-drive-desktop-packages',
        properties: {
          packages: {
            type: Array,
            computed: '_computeUrls(_tp)'
          },
          _tp: String
        },
        behaviors: [Nuxeo.I18nBehavior],

        _computeUrls: function(tp) {
          if (!tp) {
            return;
          }
          var pkgs = [];

          var prefix = 'nuxeo-drive';
          var baseUrl = 'https://community.nuxeo.com/static/drive-updates';

          var name = prefix + '.dmg';
          pkgs.push({
            name: name,
            platform: 'osx',
            url: baseUrl + '/' + name
          });

          name = prefix + '.exe';
          pkgs.push({
            name: name,
            platform: 'windows',
            url: baseUrl + '/' + name
          });

          pkgs.push({
            name: window.nuxeo.I18n.translate('driveDesktopPackages.ubuntu.name',
                                              'Read the documentation about the Linux client'),
            platform: 'ubuntu',
            url: 'https://github.com/nuxeo/nuxeo-drive#debian-based-distributions-and-other-gnulinux-variants-client'
          });
          return pkgs;
        }
      });
    })();
  </script>
</dom-module>
