(function() {
        // Load plugin specific language pack
        tinymce.PluginManager.requireLangPack('nuxeolink');

        tinymce.create('tinymce.plugins.NuxeoLinkPlugin', {

                init : function(ed, url) {
                        // Register the command so that it can be invoked by using tinyMCE.activeEditor.execCommand('mceExample');
                        ed.addCommand('mceNuxeoLink', function() {
								var template = new Array();
								var urlPlugin = nxContextPath + '/editor_link_search_document.faces' + '?conversationId=' + currentConversationId + '&conversationIsLongRunning=true';
								window.open(urlPlugin, '_blank', 'toolbar=0, scrollbars=1, location=0, statusbar=0, menubar=0, resizable=1, dependent=1, width=800, height=480');
								return true;
                        });

                        // Register example button
                        ed.addButton('nuxeolink', {
                                title : 'nuxeolink.desc',
                                cmd : 'mceNuxeoLink',
								image : url + '/images/icon.gif'
                        });
                        // Add a node change handler, selects the button in the UI when a image is selected
                       // ed.onNodeChange.add(function(ed, cm, n) {
                         //       cm.setActive('NuxeoImageUploadPlugin', n.nodeName == 'http://forum.nuxeo.com/theme/nuxeo/images/existing_content.png');
                        //});
                },

                /**
                 * Creates control instances based in the incomming name. This method is normally not
                 * needed since the addButton method of the tinymce.Editor class is a more easy way of adding buttons
                 * but you sometimes need to create more complex controls like listboxes, split buttons etc then this
                 * method can be used to create those.
                 *
                 * @param {String} n Name of the control to create.
                 * @param {tinymce.ControlManager} cm Control manager to use inorder to create new control.
                 * @return {tinymce.ui.Control} New control instance or null if no control was created.
                 */
                //createControl : function(n, cm) {
                  //      return null;
                //},

                /**
                 * Returns information about the plugin as a name/value array.
                 * The current keys are longname, author, authorurl, infourl and version.
                 *
                 * @return {Object} Name/value array containing information about the plugin.
                 */
                getInfo : function() {
                        return {
                                longname : 'Nuxeo',
                                author : 'Nuxeo',
                                authorurl : 'http://www.nuxeo.com',
                                infourl : 'http://www.nuxeo.com',
                                version : "3.0"
                        };
                }
        });

        // Register plugin
        tinymce.PluginManager.add('nuxeolink', tinymce.plugins.NuxeoLinkPlugin);
})();