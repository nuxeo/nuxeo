//jsPlumb options
var dynamicAnchors = [ 0.5, 0.25, 0.75, 0, 1, 0.375, 0.625, 0.125, 0.875 ];

var connectionColors = [ "#92e1aa", "#F7BE81", "#BDBDBD", "#5882FA", "#E1F5A9",
		"#FA5858", "#FFFF00", "#FF0000", "#D8F781" ];

var sourceEndpointOptions = {
	connector : [ "Flowchart", { cornerRadius: 5 } ],
	paintStyle : {
		fillStyle : '#92e1aa'
	},
	isSource : true,
	isTarget : false,
	uniqueEndpoint : true,
	maxConnections : 1
};

var targetEndpointOptions = {
	paintStyle : {
		fillStyle : '#003f7d'
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
		PaintStyle : {
			strokeStyle : "#92e1aa",
			lineWidth : 3,
			outlineWidth : 2,
			outlineColor : "white",
			joinstyle : "round"
		},
		Endpoint : [ "Dot", {
			radius : 6
		} ],
		ConnectionOverlays : [ [ "Arrow", {
			location : 0.8
		}, {
			foldback : 0.9,
			fillStyle : "#92e1aa",
			width : 14
		} ] ]
	});
};
function getConnectionOverlayLabel(colour, condition) {
	return [ [ "Arrow", {
		location : 0.8
	}, {
		foldback : 0.9,
		fillStyle : "#92e1aa",
		width : 14
	} ], [ "Label", {
		label : "<span title=\"" + condition + "\">" + condition + "</span>",
		cssClass : "workflow_connection_label",
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
		var node = '<div class="workflow_node" id="' + this.id + '">' + this.title
				+ '</div>';
		var el = jQuery(node).appendTo('#' + divContainerTargetId).css(
				'position', 'absolute').css('left', this.x).css('top', this.y);

		if (this.isStartNode) {
			el.addClass('workflow_start_node');
		} else if (this.isEndNode) {
			el.addClass('workflow_end_node');
		} else if (this.isMerge) {
			el.addClass('workflow_merge_node');
		} else if (this.isMultiTask) {
			el.addClass('workflow_multiple_task');
		} else if (this.hasSubWorkflow) {
			el.addClass('workflow_subworkflow_task');
		} else {
			el.addClass('workflow_simple_task');
		}
		if (this.state == 'suspended') {
			el.addClass('workflow_node_suspended');
		}

	});
	// initialize connection source points
	var nodes = [];

	// determine number of source endpoints per node
	var sourceEndpoints = {};
	jQuery.each(data['transitions'], function() {
		sourceEndpoints[this.nodeSourceId] = (sourceEndpoints[this.nodeSourceId] || 0) + 1;
	});

	// use fixed dynamic anchors, only 9 items supported, after this everything
	// is displayed on the center
	jQuery.each(data['transitions'], function() {
		var anchorIndex = countElement(this.nodeSourceId, nodes);
		if (anchorIndex > 9) {
			anchorIndex = 0;
		}
		nodes.push(this.nodeSourceId);
		// determine anchors for this node
		var anchors = dynamicAnchors.slice(0, sourceEndpoints[this.nodeSourceId]).sort();
		// increase index
		var endPointSource = jsPlumb.addEndpoint(this.nodeSourceId, {
			anchor : [ anchors[anchorIndex], 1, 0, 1 ]
		}, sourceEndpointOptions);
		var endPointTarget = jsPlumb.addEndpoint(this.nodeTargetId, {
			anchor : "TopCenter"
		}, targetEndpointOptions);
		// prepare the transition's path
		// ignore paths with only one segment
		if (this.path && this.path.length > 2) {
			var segments = [];
			for (var i = 1; i < this.path.length; i++) {
				segments.push({
					start: [this.path[i - 1].x, this.path[i - 1].y],
					end: [this.path[i].x, this.path[i].y]
				});
			}
		}
		jsPlumb.connect({
			source : endPointSource,
			target : endPointTarget,
			overlays : getConnectionOverlayLabel(connectionColors[anchorIndex],
					this.label),
			paintStyle : {
				lineWidth : 3,
				strokeStyle : connectionColors[anchorIndex],
				outlineWidth : 2,
				outlineColor : "white",
				joinstyle : "round"
			},
			detachable:false,
			path: segments
		});
	});
	jQuery(document.getElementById(divContainerTargetId)).append(
            "<input type='hidden' name='graphInitDone' value='true' />");
};

function invokeGetGraphOp(routeId, currentLang, divContainerTargetId) {
	var automationCtx = {};
	var options = {repository : ctx.repository };
	var getGraphNodesExec = jQuery().automation('Document.Routing.GetGraph', options);
	getGraphNodesExec.setContext(automationCtx);
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