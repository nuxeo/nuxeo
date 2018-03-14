
(: the URI of the document to update :)
declare variable $uri as xs:string external;

(: an XML version of a StateDiff :)
declare variable $patch-string as xs:string external;

(: parse the incoming XML :)
declare variable $patch as element() := xdmp:unquote($patch-string)/*;

(::
 : Updates the given $document using spec in $patch. $document must be
 : an in-database document.
 ::)
declare function local:patch-document(
  $document as element(),
  $patch as element()
) {
  for $key in $patch/*
  let $node-name := fn:node-name($key)
  let $target := ($document/*[fn:node-name(.) = $node-name])[1] (: assume there can only be one instance of a key :)
  return
    if (fn:string($key) eq "NULL") then
      xdmp:node-delete($target)
    else if (fn:empty($target)) then
      (: Object does not exist in document :)
      xdmp:node-insert-child($document, $key)
    else if (fn:not(fn:empty($key/*))) then
      (: must be a sub element :)
      let $sub-document := $target
      let $sub-patch := $key
      return
        if (fn:exists($sub-patch/diff) and fn:exists($sub-patch/rpush)) then (: have to have a "diff" and "rpush" element? :)
        (
          for $diff at $index in $sub-patch/diff/*
          let $node-name := fn:node-name($diff)
          let $target := $sub-document/*[fn:node-name(.) = $node-name][$index]
          return
            if (fn:empty($diff/*) and fn:string($diff) eq "NULL") then
              (: delete the node if and only if we receive NULL as value and it's not a complex element :)
              (: fn:string of <a><b>NULL</b></a> will return NULL :)
              xdmp:node-delete($target)
            else if (fn:string($diff) eq "NOP") then
              () (: leave it alone :)
            else if (fn:not(fn:empty($diff/*))) then
              (: Loop on sub structure :)
              local:patch-document($target, $diff)
            else if (fn:empty($target)) then
              (: there is no matching list entry so just add it? :)
              xdmp:node-insert-child($sub-document, $diff)
            else
              (: Primitive value :)
              xdmp:node-replace($target, $diff),

          for $rpush at $index in $sub-patch/rpush/*
          return
            xdmp:node-insert-child($sub-document, $rpush)
        )
        else if (fn:exists($sub-patch/*[fn:ends-with(fn:name(.), "__item")])) then
          (: Replace list/array :)
          xdmp:node-replace($sub-document, $sub-patch)
        else
          (: Loop on sub structure :)
          local:patch-document($sub-document, $sub-patch)
    else
      (: Primitive value :)
      xdmp:node-replace($target, $key)
};

let $document := fn:doc($uri)/*
return (
  local:patch-document($document, $patch)
)
