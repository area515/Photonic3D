(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrinterControlsController", ['$scope', '$http', '$location', '$routeParams', function ($scope, $http, $location, $routeParams) {
		controller = this;
		
		function createWebSocketURL(relativeURL) {
			  var loc = window.location;
			  var schema;
			  if (loc.protocol === "https:") {
				  schema = "wss:";
			  } else {
				  schema = "ws:";
			  }
			  
			  return schema + "//" + loc.host + "/services/" + relativeURL;
		}

		function attachToPrinter(printerName) {
		  	if ("WebSocket" in window) {
		  		printerName = encodeURIComponent(printerName);
		  		
				var ws = new WebSocket(createWebSocketURL("printerNotification/" + printerName));
				//ws.onopen = function() {};//Do nothing on open...
				ws.onmessage = function createJobWebSocket(event) {
						 var printerEvent = JSON.parse(event.data);
						 controller.currentPrinter = printerEvent.printer;
				};
				//TODO: On error reconnect!!
				ws.onclose = function() {
				 	$scope.$on('$destroy', function() {
				        ws.close();
				    });
				};
			}
		}
		
		var printerName = $location.search().printerName;
		$http.get("/services/printers/get/" + printerName).success(
        		function (data) {
        			controller.currentPrinter = data;
        		})
        		
		attachToPrinter(printerName);
	}])

})();