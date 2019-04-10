var nuxeo = nuxeo || {}

nuxeo.suggestbox = (function(m) {

  var absoluteUrlRegExp = /^(?:[a-z]+:)?\/\//;

  var entityMap = {
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          '"': '&quot;',
          "'": '&#39;',
          '/': '&#x2F;'
        };

  function escapeHTML(string) {
    return String(string).replace(/[&<>"'\/]/g, function fromEntityMap (s) {
      return entityMap[s];
    });
  };

  m.selectedFormatter = function(item) {
    return '';
  };

  m.suggestedFormatter = function(item) {
    return '<span><img src="' + nxContextPath + item.icon + '" class="smallIcon" />' + escapeHTML(item.label) + '</span>';
  };

  m.entryHandler = function(item) {
    var docUrl = item.url;
    if (!docUrl.match(absoluteUrlRegExp)) {
      docUrl = baseURL + docUrl;
    }
    if (typeof currentConversationId != 'undefined') {
      docUrl += "?conversationId=" + currentConversationId;
    }
    window.location.replace(docUrl);
  };

  m.enterKeyHandler = function(search) {
    var searchUrl;
    window.alert("#{searchUIActions.searchPermanentLinkUrl}");
    //window.location.replace(searchUrl);
  };

  return m;

}(nuxeo.suggestbox || {}));
