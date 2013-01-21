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
    if (event && event.srcElement) {
      var ele = jQuery(event.srcElement)
      if (ele.is('input') && ele.attr('type').match(/checkbox/i)) {
        return false
      }
    }
    return true
  }

  m.afterDocumentSelected = function(event) {
    removeAllSelectedItemClass()
    jQuery(event.currentTarget).addClass('selectedItem');
  }

  return m

}(nuxeo.dam || {}))
