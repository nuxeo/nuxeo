var nuxeo = nuxeo || {}

nuxeo.suggestbox = (function(m) {
	
	m.selectedFormatter = function(item) {
		return '';
	}
	
	m.suggestedFormatter = function(item) {
		return '<span><img src="/nuxeo'+ item.icon +'" />'+ item.label + '</span>'
	}
	
	m.entryHandler = function(item) {
		var docUrl = item.url;
        if (typeof currentConversationId != 'undefined') {
          docUrl += "?conversationId=" + currentConversationId;
        }
        window.location.replace(docUrl);
	}
	
	m.enterKeyHandler = function(search) {
		var searchUrl;
		window.alert("#{searchUIActions.searchPermanentLinkUrl}");
		//window.location.replace(searchUrl);
	}
	
	return m

}(nuxeo.suggestbox || {}));
