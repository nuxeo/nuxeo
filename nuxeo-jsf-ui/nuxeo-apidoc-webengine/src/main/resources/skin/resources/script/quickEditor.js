
var editorDisplayed=false;

function getTargetUrl(id,target) {
    var targetUrl = document.location.href
    // remove Query String
    if (targetUrl.indexOf("?")>0) {
      targetUrl = targetUrl.substr(0,targetUrl.indexOf("?"));
    }
    // if listing mode then call on the distribution object
    if (targetUrl.indexOf("/list")>0) {
        targetUrl = targetUrl.substr(0,targetUrl.indexOf("/list"));
        targetUrl = targetUrl + '/viewArtifact/' + target;
      }
    if (targetUrl.substr(-1)!="/") {
     targetUrl +="/";
    }
    targetUrl += 'quickEdit/' + id;

    return targetUrl;
  }

function quickEditSave(id, target) {

 var targetUrl = getTargetUrl(id,target);

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
  $('#liveQuickEditoSave').remove();
  $('#liveQuickEditorCancel').remove();

  editorDisplayed=false;
}

function quickEditShow(id, target) {

var isPlaceHolder=false;

if (id.match("^placeholder_")=="placeholder_") {
 isPlaceHolder=true;
}
if (editorDisplayed==true) {
 return;
}

var targetUrl = getTargetUrl(id,target);

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

var saveButton="<img id='liveQuickEditorSave' src='" + skinPath + "/images/save.gif' title='Save' onclick='return quickEditSave(\"" + id + "\",\"" + target + "\")' />";
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
