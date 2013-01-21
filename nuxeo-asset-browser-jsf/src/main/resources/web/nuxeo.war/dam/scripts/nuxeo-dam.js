var nuxeo = nuxeo || {}

nuxeo.dam = (function(m) {

  m.canSelectDocument = function(event) {
    if (event && event.srcElement) {
      var ele = jQuery(event.srcElement)
      if (ele.is('input') && ele.attr('type').match(/checkbox/i)) {
        return false
      }
    }
    return true
  }

  m.removeAllSelectedClass = function() {
    jQuery('.bubbleBox.selected').removeClass('selected')
  }

  m.selectDocument = function(event, docRef) {
    if (nuxeo.dam.canSelectDocument(event)) {
      selectDocument(docRef)
    }
  }

  return m

}(nuxeo.dam || {}))
