var nuxeo = nuxeo || {};
nuxeo.utils = nuxeo.utils || {};

nuxeo.utils.DropboxPicker = function(pickId, inputId, infoId) {

    var options = {
      success: function(files) {
        var doc = files[0];
        if (inputId) {
          document.getElementById(inputId).value = doc.link ;
        }
        if (infoId) {
          document.getElementById(infoId).innerHTML = '<img width="16" height="16" src="' + doc.icon
              + '"/> ' + doc.name;
        }
      },

      cancel: function() {
      },

      // "preview" is a preview link to the document for sharing,
      // "direct" is an expiring link to download the contents of the file.
      linkType: "direct"
    };

    // open picker
    Dropbox.choose(options);
};
