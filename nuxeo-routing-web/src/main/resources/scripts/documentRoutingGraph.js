//jsPlumb options
var dynamicAnchors = [ [ 0.5, 1, 0, 1 ], [ 0.33, 1, 0, 1 ], [ 0.66, 1, 0, 1 ],
		[ 0, 1, 0, 1 ], [ 1, 1, 0, 1 ], [ 0.2, 1, 0, 1 ], [ 0.8, 1, 0, 1 ],
		[ 0.1, 1, 0, 1 ], [ 0.9, 1, 0, 1 ] ];

var connectionColors = [ "#F78181", "#F7BE81", "#BDBDBD", "#5882FA", "#E1F5A9",
		"#FA5858", "#FFFF00", "#FF0000", "#D8F781" ];

var sourceEndpointOptions = {
	connector : [ "Flowchart" ],
	paintStyle : {
		fillStyle : '#F78181'
	},
	isSource : true,
	isTarget : false,
	uniqueEndpoint : true,
	maxConnections : 1
};

var targetEndpointOptions = {
	paintStyle : {
		fillStyle : '#B23838'
	},
	isSource : false,
	isTarget : true,
	reattach : true,
	// without specifying this the targetEndpoint doesn't accept multiple
	// connections
	maxConnections : -1
};

function jsPlumbInitializeDefault() {
	jsPlumb.importDefaults({
		DragOptions : {
			cursor : "pointer",
			zIndex : 2000
		},
		Endpoint : [ "Dot", {
			radius : 6
		} ],
		HoverPaintStyle : {
			strokeStyle : "#ec9f2e"
		},
		EndpointHoverStyle : {
			fillStyle : "#ec9f2e"
		},
		ConnectionOverlays : [ [ "Arrow", {
			location : 0.8
		}, {
			foldback : 0.9,
			fillStyle : "#F78181",
			width : 14
		} ] ]
	});
};
function getConnectionOverlayLabel(colour, condition) {
	return [ [ "Arrow", {
		location : 0.8
	}, {
		foldback : 0.9,
		fillStyle : colour,
		width : 14
	} ], [ "Label", {
		label : "<span title=\"" + condition + "\">" + condition + "</span>",
		cssClass : "node_connection_label",
		location : 0.6
	} ] ];
}
// --> end jsPlumbOptions
// display graph
function countElement(item, array) {
	var count = 0;
	jQuery.each(array, function(i, v) {
		if (v === item)
			count++;
	});
	return count;
};
function displayGraph(data, divContainerTargetId) {
	jQuery.each(data['nodes'], function() {
		var node = '<div class="node" id="' + this.id + '">' + this.title
				+ '</div>';
		var el = jQuery(node).appendTo('#' + divContainerTargetId).css(
				'position', 'absolute').css('left', this.x).css('top', this.y);

		if (this.isStartNode) {
			el.addClass('start_node');
		} else if (this.isEndNode) {
			el.addClass('end_node');
		} else if (this.isMerge) {
			el.addClass('merge_node');
		} else if (this.isMultiTask) {
			el.addClass('multiple_task');
		} else if (this.hasSubWorkflow) {
			el.addClass('subworkflow_task');
		} else {
			el.addClass('simple_node');
		}
		if (this.state == 'suspended') {
			el.addClass('node_suspended');
		}

	});
	// initialize connection source points
	var nodes = [];
	// use fixed dynamic anchors, only 9 items supported, after this everything
	// is displayed on the center
	jQuery.each(data['transitions'], function() {
		var anchorIndex = countElement(this.nodeSourceId, nodes);
		if (anchorIndex > 9) {
			anchorIndex = 0;
		}
		;
		nodes.push(this.nodeSourceId);
		// increase index
		var endPointSource = jsPlumb.addEndpoint(this.nodeSourceId, {
			anchor : dynamicAnchors[anchorIndex]
		}, sourceEndpointOptions);
		var endPointTarget = jsPlumb.addEndpoint(this.nodeTargetId, {
			anchor : "TopCenter"
		}, targetEndpointOptions);
		jsPlumb.connect({
			source : endPointSource,
			target : endPointTarget,
			overlays : getConnectionOverlayLabel(connectionColors[anchorIndex],
					this.label),
			paintStyle : {
				lineWidth : 1,
				strokeStyle : connectionColors[anchorIndex]
			}
		});
	});
};

function invokeGetGraphOp(routeId, currentLang, divContainerTargetId) {
	var ctx = {};
	var getGraphNodesExec = jQuery().automation('Document.Routing.GetGraph');
	getGraphNodesExec.setContext(ctx);
	getGraphNodesExec.addParameter("routeDocId", routeId);
	getGraphNodesExec.addParameter("language", currentLang);
	getGraphNodesExec.executeGetBlob(function(data, status, xhr) {
		displayGraph(data, divContainerTargetId);
	}, function(xhr, status, errorMessage) {
		jQuery('<div>Can not load graph </div>').appendTo(
				'#' + divContainerTargetId);
	}, true);
};

function loadGraph(routeDocId, currentLang, divContainerTargetId) {
	jsPlumbInitializeDefault();
	invokeGetGraphOp(routeDocId, currentLang, divContainerTargetId);
};