
var editorDisplayed=false;

function quickEditSave(id) {

 var targetUrl = document.location.href
 if (targetUrl.substr(-1)!="/") {
  targetUrl +="/";
 }
 targetUrl += 'quickEdit/' + id;

 var content = $('#liveQuickEditor').val();
 var title=$('#liveQuickEditorTitle').val();

$.post(targetUrl, {id: id, title: title, content: content} ,function(data) {
  document.location.href=document.location.href;
});

}

function quickEditCancel(id) {

  if (!confirm("Are you sure you want to discard changes ?")) {
    return;
  }
  var titleItem = $("span").filter(function() { return this.id==(id+'_doctitle');})[0]
  var textItem = $("div").filter(function() { return this.id==(id+'_doccontent');})[0]
  var editBtn = $("img").filter(function() { return this.id==(id+'_button');})[0]

  $(titleItem).css("display","block");
  $(textItem).css("display","block");
  $(editBtn).css("display","block");

  $('#liveQuickEditor').remove();
  $('#liveQuickEditorTitle').remove();
  $('#liveQuickEditorSave').remove();
  $('#liveQuickEditorCancel').remove();

  editorDisplayed=false;
}

function quickEditShow(id) {

 var targetUrl = document.location.href
 if (targetUrl.substr(-1)!="/") {
  targetUrl +="/";
 }
 targetUrl += 'quickEdit/' + id;


var isPlaceHolder=false;

if (id.match("^placeholder_")=="placeholder_") {
 isPlaceHolder=true;
}
if (editorDisplayed==true) {
 return;
}
editorDisplayed=true;

var titleItem = $("span").filter(function() { return this.id==(id+'_doctitle');})[0]
var textItem = $("div").filter(function() { return this.id==(id+'_doccontent');})[0]

var editTitle="<input class='quickEdit' id='liveQuickEditorTitle' type='text' size='50' value='" + $(titleItem).html() + "' >";
$(editTitle).insertBefore($(titleItem));
$(titleItem).css("display","none");

var textContent="Loading...";
if (isPlaceHolder) {
  textContent="";
}

var editContent="<textarea id='liveQuickEditor' class='quickEdit' cols='120' rows='20' >" + textContent + "</textarea>";
$(editContent).insertBefore($(textItem));
$(textItem).css("display","none");

var saveButton="<img id='liveQuickEditorSave' src='" + skinPath + "/images/save.gif' title='Save' onclick='return quickEditSave(\"" + id + "\")' />";
var cancelButton="<img id='liveQuickEditorCancel' src='" + skinPath + "/images/cancel.png' title='Cancel' onclick='return quickEditCancel(\"" + id + "\")' />";

var editBtn = $("img").filter(function() { return this.id==(id+'_button');})[0]
$(cancelButton).insertAfter($(editBtn));
$(saveButton).insertAfter($(editBtn));
$(editBtn).css("display","none");

if (isPlaceHolder) {
  return;
}

$.get(targetUrl, function(data) {
  $('#liveQuickEditor').attr("enabled","false");
  $('#liveQuickEditor').html(data);
});



return true;
}


