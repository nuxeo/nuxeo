<!--
(C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<link rel="import" href="nuxeo-template-param-editor.html">

<!--
`nuxeo-delete-template-button`
@group Nuxeo UI
@element nuxeo-delete-template-button
-->
<dom-module id="nuxeo-delete-template-button">
  <template>
    <style include="nuxeo-styles nuxeo-action-button-styles">
      .container {
        padding: 1em 0 2em;
      }

      .buttons {
        @apply --buttons-bar;
      }
    </style>

    <nuxeo-operation id="deleteTemplatesOp" op="TemplateProcessor.Detach"></nuxeo-operation>
    <nuxeo-document id="template" doc-path="[[document.path]]"></nuxeo-document>
    <div id="delete" class="action" on-tap="_toggleDialog">
      <paper-icon-button icon="icons:delete-sweep" noink ></paper-icon-button>
      <span class="label" hidden$="[[!showLabel]]">[[i18n('deleteTemplateButton.tooltip')]]</span>
    </div>
    <paper-tooltip for="delete">[[i18n('deleteTemplateButton.tooltip')]]</paper-tooltip>

    <nuxeo-dialog id="dialog" modal no-auto-focus>
      <h2>[[i18n('deleteTemplateButton.dialog.heading')]]</h2>
      <paper-dialog-scrollable>
        <div class="container horizontal layout">
          <span>[[i18n('deleteTemplateButton.dialog.message')]]</span>
        </div>
      </paper-dialog-scrollable>
      <div class="buttons horizontal end-justified layout">
        <div class="flex start-justified">
          <paper-button noink dialog-dismiss>[[i18n('command.cancel')]]</paper-button>
        </div>
        <paper-button noink class="primary" on-tap="_delete">
          [[i18n('deleteTemplateButton.dialog.confirm')]]
        </paper-button>
      </div>
    </nuxeo-dialog>

  </template>

  <script>
    Polymer({
      is: 'nuxeo-delete-template-button',
      behaviors: [Nuxeo.I18nBehavior, Nuxeo.RoutingBehavior],
      properties: {
        /**
         * The template to be deleted
         **/
        document: Object,
        /**
         * `true` if the action should display the label, `false` otherwise.
         **/
        showLabel: {
          type: Boolean,
          value: false,
        }
      },

      _toggleDialog: function() {
        this.$.dialog.toggle();
      },

      _delete: function() {
        this.$.deleteTemplatesOp.input = this.document.uid;
        this.$.deleteTemplatesOp.execute().then(this.$.template.remove.bind(this.$.template))
                                          .then(function() {
                                            window.location = this.urlFor('document', this.document.parentRef);
                                          }.bind(this));
      }

    });
  </script>

</dom-module>
