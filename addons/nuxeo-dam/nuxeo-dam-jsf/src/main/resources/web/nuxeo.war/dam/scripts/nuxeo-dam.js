var nuxeo = nuxeo || {}

nuxeo.dam = (function(m) {

  function removeAllSelectedItemClass() {
    jQuery('.jsDamItem.selectedItem').removeClass('selectedItem')
  }

  m.selectDocument = function(event, docRef) {
    if (nuxeo.dam.canSelectDocument(event)) {
      // trigger the a4j:jsFunction
      damSelectDocument(docRef)
    }
  }

  m.canSelectDocument = function(event) {
    if (event && event.target) {
      var ele = jQuery(event.target)
      if (ele.is('input') && ele.attr('type').match(/checkbox/i)) {
        return false
      }
    }
    return true
  }

  m.afterDocumentSelected = function(data) {
    removeAllSelectedItemClass()
    jQuery("[data-docref='" + data + "']").addClass('selectedItem')
  }

  return m

}(nuxeo.dam || {}))
