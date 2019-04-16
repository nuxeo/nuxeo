module namespace extract = "http://nuxeo.com/extract";

(::
 : Extracts nodes from the node provided in $node parameter based on the paths
 : provided in the $paths parameter. The paths can anything supported by the
 : path range index XPath expressions. See https://docs.marklogic.com/guide/admin/range_index#id_40666
 : for details on what is supported. The $namespaces parameter is used to
 : bind namespace prefixes used in the XPath. See https://docs.marklogic.com/xdmp:with-namespaces
 : for details on what can be passed in.
 :
 : An empty sequence will be returned if there are no matching nodes for the
 : given path.
 ::)
declare function extract:extract-nodes(
	$node as node(),
	$paths as xs:string*,
	$namespaces
) as node()*  {
	let $expression := concat("$node/((.", string-join($paths, ") | (."), "))")
	let $nodes := xdmp:with-namespaces($namespaces, xdmp:value($expression))
	let $ancestors := $nodes/ancestor::node() except $nodes
	return
		extract:select-nodes($node, $nodes, $ancestors)
};

declare private function extract:select-nodes(
	$nodes as node()*,
	$selected as node()*,
	$ancestors as node()*
) as node()* {
	for $n in $nodes
	return
		if ($n is $selected) then
			$n
		else if ($n is $ancestors) then
			typeswitch ($n)
				case document-node() return
					document {
						extract:select-nodes($n/node(), $selected, $ancestors)
					}
				case element() return
					element { fn:node-name($n) } {
						extract:select-nodes(($n/@*, $n/node()), $selected, $ancestors)
					}
				default return
					$n
		else
			()
};
