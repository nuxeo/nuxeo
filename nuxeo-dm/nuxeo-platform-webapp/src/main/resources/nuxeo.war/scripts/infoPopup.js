jQuery.noConflict();

function showInfoMessagePopup(messageContentId) {


// build popup
var frame = jQuery("<div></div>").css({border:'2px', backgroundColor:'white', borderStyle:'outset', borderColor:'black'}).hide().appendTo('body')
var border = jQuery("<div style='text-align:right'></div>").css({backgroundColor:'#CCCCCC', cursor:'hand'})
var close = jQuery("<img src='icons/action_delete_mini.gif'/>").css({cursor:'hand'})
close.click(function(e) { frame.hide()})
close.appendTo(border)
border.appendTo(frame)
var content = jQuery("<div></div>").css({padding:'2px'}).appendTo(frame)
var popup = jQuery('#' + messageContentId).clone(true)
popup.show()
content.html(popup);
frame.css({position:'absolute', zIndex:'500'})

// handle centering
var windowWidth = document.documentElement.clientWidth;
var windowHeight = document.documentElement.clientHeight;
var popupW = frame.width();
var popupH = frame.height();
var popupTop = windowHeight/2 - popupH/2;
var popupLeft = windowWidth/2 - popupW/2;
if (popupTop<=0) {
  popupTop=10;
}
if (popupLeft<=0) {
  popupLeft=10;
}

frame.css({top: popupTop, left: popupLeft})

// show
frame.fadeIn()
}
