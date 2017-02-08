function setupDisplay(jqSelect, initVal) {
    var inputType = jqSelect.val();
    if (inputType=='xPath') {
      jqSelect.next().css('display','inline');
      jqSelect.prev().val(initVal);
      jqSelect.next().val(initVal);
    } else {
      jqSelect.next().css('display','none');
      jqSelect.prev().val(inputType);
    }
}

function initContentIncludeWidget(index, widget) {
  var select = jQuery(widget).children("select")[0];  
  select = jQuery(select);

  // bind onChange
  select.change(function() {
    setupDisplay(jQuery(this));
  });
  // key binding to copy content
  select.next().keyup( function() {
     jQuery(this).prev().prev().val(jQuery(this).val());
  });

  // init
  var initVal = select.prev().val();  
  if (initVal == 'htmlPreview' || initVal == 'blobContent') {
    select.val(initVal);    
  } else {
    select.val('xPath');    
  }
  setupDisplay(select, initVal);

}

function initContentWidgets() {
 jQuery(".contentWidget").each( function ( index, widget) {
    initContentIncludeWidget(index, widget);
  });
}

