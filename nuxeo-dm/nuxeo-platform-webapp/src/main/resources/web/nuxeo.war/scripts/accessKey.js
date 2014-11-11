function showAccessKeys() {
    if (jQuery(".accessKeyToolTip").size()>0) {
        jQuery(".accessKeyToolTip").remove();
        return;
    }
    jQuery("[accesskey]").each(function() {
        var item = jQuery(this);
        var key = item.attr("accesskey");
        if (key !=null && key !="" && this.innerHTML!="") {
              var tooltip = jQuery("<span>" + key + "</span>");
              jQuery(tooltip).css({"background-color":"#666666", "color":"white","padding":"3px", "margin":"2px","border-radius" : "2px", "font-size" : "9px"});
              jQuery(tooltip).addClass("accessKeyToolTip");
              item.append(tooltip);
         }
    });
}
jQuery(document).bind('keydown', 'Shift+h', showAccessKeys);
