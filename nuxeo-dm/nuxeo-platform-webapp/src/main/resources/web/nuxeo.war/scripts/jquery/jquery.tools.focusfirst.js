(function($) {
    $.fn.focusFirst = function(){
        var topElement=$(this).get(0);
        if(topElement === undefined){
          return this;
        }
        var topElementId=topElement.getAttribute("id");
        
        /** Compute the absolute offset of a component by recursively climbing the component three.
        If a topElement is given, recursion will stop once reaching a component with that given ID. */
        function GetOffset (object, offset, topElement) {
            if (!object || object.getAttribute("id")==topElement)
                return;
            offset.x += object.offsetLeft;
            offset.y += object.offsetTop;
            GetOffset (object.parentNode, offset, topElement);
        }
        
        var elem=$('input:visible', this).get(0);
        var select=$('select:visible', this).get(0);
        if(select&&elem){
            var elemOffset = {'x':0,'y':0};
            var selectOffset = {'x':0,'y':0};
            GetOffset(elem, elemOffset, topElementId);
            GetOffset(select, selectOffset, topElementId);
            
            if(selectOffset.y < elemOffset.y){
                elem=select;
            }
        }
        
        var textarea=$('textarea:visible', this).get(0);
        if(textarea&&elem){
            var elemOffset = {'x':0,'y':0};
            var textOffset = {'x':0,'y':0};
            GetOffset(elem, elemOffset, topElementId);
            GetOffset(textarea, textOffset, topElementId);
            
            if(textOffset.y < elemOffset.y){
                elem=textarea;
            }
        }
        if(elem){
            try{
                elem.focus();
            }
            catch(err){}
        }
        return this;
    };

})(jQuery);