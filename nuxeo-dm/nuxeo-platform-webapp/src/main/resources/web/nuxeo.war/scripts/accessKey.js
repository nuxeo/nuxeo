function showAccessKeys() {
    if (jQuery(".accessKeyMenu").size()>0) {
        jQuery(".accessKeyMenu").remove();
        return;
    }
    var container = jQuery("<div></div>");
    container.css("display","none");
    var div = jQuery("<div></div>");
    div.attr("id","accessKeyMenuPopup");
    div.addClass("accessKeyMenu");
    div.css({"padding":"8px", "margin":"4px", "font-size" : "12px"});
    container.append(div);

    var table = jQuery("<table></table>");
    div.append(table);

    jQuery("[accesskey]").each(function() {
        var item = jQuery(this);
        var key = item.attr("accesskey");


        if (key !=null && key !="") {
              var row = jQuery("<tr></tr>");
              var keySpan = jQuery("<span>" + key + "</span>");
              keySpan.css({"background-color":"#CCCCCC", "color":"black","padding":"6px", "margin":"2px","border-radius" : "2px", "font-size" : "12px", "font-weight" : "bold", "font-family": "monospace"});
              var keyText = this.innerHTML;
              if (this.tagName=="INPUT" && (item.attr("type")=="button" || item.attr("type")=="submit")) {
                 keyText = item.attr("value");
              }
              if (keyText && keyText!="" && keyText.indexOf("<!--")!=0 ) {
                var td = jQuery("<td></td>");
                td.css({"padding":"6px"});
                td.append(keySpan);
                row.append(td);

                var descSpan = jQuery("<span></span>");
                descSpan.css({"white-space":"nowrap"});
                descSpan.append(keyText);

                td = jQuery("<td></td>");
                td.append(descSpan);
                row.append(td);

                table.append(row);
              }
         }
    });
    jQuery("body").append(container);
    showFancyBox("#accessKeyMenuPopup");
}

function bindShortCuts() {
    // bind access keys to Ctrl+Shift+
    jQuery("[accesskey]").each(function() {
        var item = jQuery(this);
        var key = item.attr("accesskey");
        if (key !=null && key !="") {
              var newKeyCode = "Ctrl+Shift+" + key;
              var clickHandler = function(event) {event.preventDefault();item[0].click();return false;};
              // Document wide binding
              jQuery(document).bind('keydown', newKeyCode, clickHandler);
              // add bindings on all inputs
              jQuery("INPUT,TEXTAREA,SELECT").bind('keydown', newKeyCode, clickHandler);
              var mceFrames = jQuery(".mceIframeContainer > IFRAME").contents().find("body");
              mceFrames.bind('keydown', newKeyCode, clickHandler);
         }
    });
    // bind help screen
    jQuery(document).bind('keydown', 'Shift+h', showAccessKeys);
}

// run binding on document ready
jQuery(document).ready(function() {
     // wait for all other onready event to do their work before we tweak the binding
     // this is needed for TinyMce
     window.setTimeout(bindShortCuts, 1000);
});

