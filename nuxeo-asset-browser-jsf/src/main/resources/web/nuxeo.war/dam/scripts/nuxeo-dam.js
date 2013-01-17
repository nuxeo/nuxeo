var nuxeo = nuxeo || {}

nuxeo.dam = (function(m) {

  m.selectDocument = function(event, docRef) {
    if (event && event.srcElement) {
      var ele = jQuery(event.srcElement)
      if (ele.is("input") && ele.attr("type").match(/checkbox/i)) {
        return
      }
    }
    // trigger the a4j:jsFunction actually selecting the document
    //noinspection JSUnresolvedFunction
    selectDocument(docRef)
  }

  return m

}(nuxeo.dam || {}))
