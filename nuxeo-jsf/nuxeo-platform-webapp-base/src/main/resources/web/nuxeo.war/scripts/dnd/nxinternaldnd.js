//***************************************************************
// JQuery wrapper to initialize internal (Prototype based)  Dnd

(function($) {

   $.fn.nxInitDraggables = function ( options ) {
     this.each(makeDraggable);
   }

   function makeDraggable() {
     var target = jQuery(this);
     //console.log("makeDraggable :" + target.attr('id'));
     new Draggable(target.attr('id'),{revert:true});
   }

   $.fn.nxInitDropTargets = function ( options ) {
     this.each(makeDropTarget);
   }

   function makeDropTarget() {
    var target = jQuery(this);
    var targetId=target.attr('id');
    var refId = targetId.replace('docRefTarget','docRef');
    if (targetId.indexOf('nodeRefTarget')==0) {
      refId = targetId.replace('nodeRefTarget','docRef');
    }
    //console.log("makeDropTarget :" + targetId + " " + refId);
    Droppables.add(targetId, {
      accept:'cell',
      onDrop:function(element)
        {moveElement(element,refId);},
      hoverclass:'dropInto'});
   }
 })(jQuery);