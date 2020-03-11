
if (typeof Document != 'object') {
  throw "Wrapper compile / Visibility";
}

if (typeof Document.Fetch != 'function') {
  throw "Wrapper compile / Visibility on Document.Fetch";
}

if (typeof localVar != 'undefined') {
  throw "Bad Isolation";
}

var localVar = "Yo";
