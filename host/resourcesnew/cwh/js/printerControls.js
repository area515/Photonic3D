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
			 	$scope.$on('$destroy', function() {
			        ws.close();
			    });
			 	
				ws.onmessage = function createJobWebSocket(event) {
						 var printerEvent = JSON.parse(event.data);
						 controller.currentPrinter = printerEvent.printer;
				};

				ws.onclose = function() {
				};
			} else {
				$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
			}
		}
		
		this.gcodeProcessing = "";
		this.gCodeToSend = "";
		var printerName = $location.search().printerName;
		$http.get("/services/printers/get/" + printerName).success(
        		function (data) {
        			controller.currentPrinter = data;
        		})
        var gCodeSuccess = function (response) {
			if (response.data.response) {
				controller.gcodeProcessing = response.data.command + ":" + response.data.message + controller.gcodeProcessing;
			} else {
				$scope.$emit("MachineResponse", {machineResponse: response.data, successFunction:null, afterErrorFunction:null});
			}
		};
		var errorFunction = function (error) {
			$scope.$emit("HTTPError", {status:error.status, statusText:error.data});
		};
		
        this.move = function move(dimension, step) {
			$http.get("services/printers/move" + dimension + "/" + printerName + "/" + step).then(gCodeSuccess, errorFunction)
		}
        this.home = function home(dimension) {
			$http.get("services/printers/home" + dimension + "/" + printerName).then(gCodeSuccess, errorFunction)
		}
        this.motor = function motor(isOn) {
			$http.get("services/printers/motors" + (isOn?"On":"Off") + "/" + printerName).then(gCodeSuccess, errorFunction)
		}
        this.executeGCode = function executeGCode(gCode) {
			$http.get("services/printers/executeGCode/" + printerName + "/" + gCode).then(gCodeSuccess, errorFunction)
		}
        this.projector = function projector(startStop) {
			$http.get("services/printers/" + startStop + "Projector/" + printerName).then(gCodeSuccess, errorFunction)
		}

		attachToPrinter(printerName);
	}])

})();