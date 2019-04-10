var nuxeo = nuxeo || {}

nuxeo.suggestbox = (function(m) {

  var absoluteUrlRegExp = /^(?:[a-z]+:)?\/\//;

  m.selectedFormatter = function(item) {
    return '';
  };

  m.suggestedFormatter = function(item) {
    return '<span><img src="/nuxeo' + item.icon + '" class="smallIcon" />' + item.label + '</span>';
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
