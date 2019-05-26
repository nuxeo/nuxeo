var nuxeo = nuxeo || {}

nuxeo.lightbox = (function(m) {

  var currentLocale;

  function getCurrentLocale() {
    if (undefined == currentLocale) {
      var cookieLocale = jQuery.cookie('org.jboss.seam.core.Locale');
      if (cookieLocale) {
        currentLocale = cookieLocale;
      } else {
        var navLang = navigator.language || navigator.userLanguage;
        if (navLang) {
          currentLocale = navLang;
        }
      }
      if (currentLocale) {
        currentLocale = currentLocale.split(new RegExp("[-_]+", "g"))[0];
      } else {
        currentLocale = 'en';
      }
    }
    return currentLocale;
  }

  function formatDocWithPicture(doc, img) {
    var creationDate = new Date(doc.properties['dc:created']);
    var maxWidth = jQuery(window).width() - 120;
    var maxHeight = jQuery(window).height() - 180;
    var markup = '<div class="mfp-figure">'
        + '<figure><img class="mfp-img" src="'
        + img
        + '" style="margin-top:-40px;max-width:'
        + maxWidth
        + 'px;max-height:'
        + maxHeight
        + 'px"><figcaption>'
        + '<div class="mfp-bottom-bar" style="top:auto;bottom:0;left:60px;position:fixed;padding-bottom:20px;max-width:'
        + maxWidth
        + 'px;"><div class="mfp-title">'
        + doc.title
        + '<small style="padding-top:5px;overflow:auto;max-height:50px">'
        + (doc.properties['dc:description'] === null ? ''
            : doc.properties['dc:description'])
        + '</small></div><div class="mfp-counter">'
        + doc.properties['dc:creator'] + ' '
        + creationDate.toLocaleDateString(getCurrentLocale()) + '</div>'
        + '</div></figcaption></figure></div>';

    return markup;
  }

  m.requestedSchema = 'dublincore, common, picture';

  m.setRequestHeaders = function(request) {
    request.setRequestHeader("X-NXDocumentProperties",
        nuxeo.lightbox.requestedSchema);
  };

  m.formatDefaultDoc = function(doc) {
    return formatDocWithPicture(doc, nxContextPath + '/img/lightbox_placeholder.png');
  };

  m.formatPictureDoc = function(doc) {
    var view;
    for (var i = 0; i < doc.properties['picture:views'].length; i++) {
        if (doc.properties['picture:views'][i].title === 'OriginalJpeg') {
            view = doc.properties['picture:views'][i];
            break;
        }
    }
    if (view === undefined) {
        return formatDefaultDoc(doc);
    }
    return formatDocWithPicture(doc,
            view.content.data);
  };

  m.formatUnknownDoc = function(doc) {
    var markup = '<div>' + '<h3>Not supported yet!</h3>' + '<div>';
    return markup;
  };

  m.formatVideoDoc = function(doc) {
    return formatDocWithPicture(doc, nxContextPath + '/img/lightbox_placeholder.png');
  };

  m.formatDoc = function(doc) {
    if (doc.facets.indexOf('Picture') > -1) {
      return nuxeo.lightbox.formatPictureDoc(doc);
    } else if (doc.facets.indexOf('Video') > -1) {
      return nuxeo.lightbox.formatVideoDoc(doc);
    } else {
      return nuxeo.lightbox.formatDefaultDoc(doc);
    }
  };

  return m;

}(nuxeo.lightbox || {}));
