var bookmarks; // This is a json object of bookmark items
var edited;
var prefs;
var permission = gadgets.nuxeo.isEditable();

function saveBookmarks() {
  prefs.set("bookmarks", gadgets.util.escapeString(gadgets.json.stringify(bookmarks)));
  var ver = navigator.appVersion;
  if (ver.indexOf("MSIE") == -1)
    _gel("newNameInput").focus();
}


function editBookmark(indexBookmark, name, url)
{
  if (indexBookmark > -1 && indexBookmark < numBookmarks()-1)
  {
    bookmarks.array[indexBookmark] = {
      "name" :name,
      "url" :formatUrl(url)
    };
    saveBookmarks();
  }

}

function addBookmark(name, url) {

  var name = _trim(name);
  _gel("newNameInput").value = "";
  _gel("newUrlInput").value = "";


  if (name == "")
    return;

  if (document.newBookmarkForm.action.value == "edit")
  {
    bookmarks.array[document.newBookmarkForm.indexTab.value] = {
          "name" :name,
          "url" :formatUrl(url)
        };
  }
  else
  {
    bookmarks.array[numBookmarks()] = {
      "name" :name,
      "url" :formatUrl(url)
    };
  }

  toogleForms();

  sortByName(numBookmarks() - 1);
  createTable();
  saveBookmarks();
  gadgets.window.adjustHeight();
  return false;
}

function formatUrl(url)
{
  url = jQuery.trim(url);
  if (url.match(/[a-z]+:\/\//)) {
    return url;
  } else {
    return "http://" + url;
  }
}

function editFormBookmark(number){

  toogleForms("edit");

  document.newBookmarkForm.indexTab.value= number;
  document.newBookmarkForm.newNameInput.value= bookmarks.array[number].name;
  document.newBookmarkForm.newUrlInput.value= bookmarks.array[number].url;


}

function deleteBookmark(number) {
  if (edited)
    return;
  if (!confirm("Etes vous sur de vouloir supprimer ce favori ?"))
    return;
  var beginning = bookmarks.array.slice(0, number);
  var end = bookmarks.array.slice(number + 1, numBookmarks());
  bookmarks.array = beginning.concat(end);
  createTable();
  saveBookmarks();
  gadgets.window.adjustHeight();
}

function swapBookmarks(to, from) {
  var temp = bookmarks.array[to];
  bookmarks.array[to] = bookmarks.array[from];
  bookmarks.array[from] = temp;
}

function sortByName(number) {
  var bookmark = bookmarks.array[number];
  ;
  var lastName = bookmark.name;
  for (i = number - 1; i >= 0; i--) {
    var currentBookmark = bookmarks.array[i];
    if (currentBookmark == null)
      break;
    var currentName = currentBookmark.name;
    if (currentName.toUpperCase() <= lastName.toUpperCase())
      break;
    swapBookmarks(i + 1, i);
  }
}

function rowClass(number) {
  if (number % 2 != 0)
    return " class=odd ";
  else
    return " class=even ";
}


function createAddBookmarkButton(){

  var html ="<a class=\"addBookmark\" href=\"javascript: toogleForms('create')\">Ajouter</a>";

  _gel("addBookmarkIcon").innerHTML = html;
  gadgets.window.adjustHeight();

}

function createTable() {
  var html = "<table cellspacing=0 id=bookmarksTable>";
  for (i = 0; i < numBookmarks(); i++) {
    if (bookmarks.array[i] == null)
      break;
    var url = bookmarks.array[i].url;
    var name = bookmarks.array[i].name;
    html = html + createRow(i, url, name);
  }
  html = html + "</table>";
  _gel("content").innerHTML = html;
  gadgets.window.adjustHeight();
}

function createRow(number, url, name) {
  var html = "<tr id=\"row" + number + "\"" + rowClass(number) + ">"
      + "<td class=name_td>" + "&nbsp;&nbsp;<a class=\"name_a\" href=\""
      + url + "\" target=\"_blank\">" + name + "</a>" + "</td>" + "<td>"
      + createEdit(number) + "</td>" + "<td>"
      + createDelete(number) + "</td>" + "</tr>";
  return html;
}

function escapeName(name) {
  name = name.replace(/&/g, "&#38;")
  name = name.replace(/</g, "&#60;")
  name = name.replace(/>/g, "&#62;")
  name = name.replace(/"/g, "&#34;")
  name = name.replace(/'/g, "&#39;")
  return name;
}

function createName(number, name) {
  var html = "<a class=name_a" + " href=\"javascript:editName(" + number
      + ")\">" + escapeName(name) + "</a>";
  return html;
}

function createEdit(number)
{
  var html;
    if (permission == true)
    {
    html = "<a class=\"editLink\" title=\"Editer le favori\" href=\"javascript:editFormBookmark("
        + number + ")\">" + "</a>";
    }
    else
    {
      html="";
    }
    return html;
}

function createDelete(number) {
  var html;
  if (permission == true)
  {
  html = "<a class=\"deleteLink\" title=\"Supprimer le favori\" href=\"javascript:deleteBookmark("
      + number + ")\">" + "</a>";
  }
  else
  {
    html="";
  }
  return html;
}

function createUrlLink(url, number) {
  var html = "<a class=p" + url + "_ " + "href=\"javascript:editUrl("
      + number + "," + url + ")\">" + getUrlText(url) + "</a>";
  return html;
}

function numBookmarks() {
  return bookmarks.array.length;
}

function toogleForms(action)
{
  document.newBookmarkForm.action.value= action;
  document.newBookmarkForm.newNameInput.value= "";
  document.newBookmarkForm.newUrlInput.value= "";

  toggleLayer('addBookmarkIcon');
  toggleLayer('addBookmarkForm');

}

function toggleLayer(whichLayer) {
  if (document.getElementById) {
    // this is the way the standards work
    var style2 = document.getElementById(whichLayer).style;
    if (whichLayer == "addBookmarkIcon") {
      style2.display = (style2.display == "block") ? "none"
          : "block";
    } else {
      style2.display = (style2.display == "block") ? "none" : "block";
    }
  } else if (document.all) {
    // this is the way old msie versions work
    var style2 = document.all[whichLayer].style;
    style2.display = (style2.display == "block") ? "none" : "block";
  } else if (document.layers) {
    // this is the way nn4 works
    var style2 = document.layers[whichLayer].style;
    style2.display = (style2.display == "block") ? "none" : "block";
  }

  gadgets.window.adjustHeight();
}
