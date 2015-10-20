//***************************************************************
// JQuery wrapper to initialize internal Dnd

(function ($) {

  $.fn.nxInitDraggables = function (options) {
    this.each(makeDraggable);
  };

  function makeDraggable() {
    var target = jQuery(this);
    //console.log("makeDraggable :" + target.attr('id'));
    target.draggable({
      revert: true
    });
  }

  $.fn.nxInitDropTargets = function (options) {
    this.each(makeDropTarget);
  };

  function makeDropTarget() {
    var target = jQuery(this);
    var targetId = target.attr('id');
    var refId = targetId.replace('docRefTarget', 'docRef');
    if (targetId.indexOf('nodeRefTarget') === 0) {
      refId = targetId.replace('nodeRefTarget', 'docRef');
    }
    //console.log("makeDropTarget :" + targetId + " " + refId);

    target.droppable({
      accept: '.cell',
      drop: function(event, ui) {
        moveElement(ui.draggable.get(0), refId);
      },
      hoverClass: 'dropInto'
    });
  }
})(jQuery);
