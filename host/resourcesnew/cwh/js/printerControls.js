(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrinterControlsController", ['$scope', '$http', '$location', '$routeParams', 'cwhWebSocket', function ($scope, $http, $location, $routeParams, cwhWebSocket) {
		controller = this;
		
		function attachToPrinter(printerName) {
			this.printerSocket = cwhWebSocket.connect("services/printerNotification/" + encodeURIComponent(printerName), $scope).onJsonContent(
				function(printerEvent) {
					controller.currentPrinter = printerEvent.printer;
				}
			);
			if (printerSocket == null) {
				$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
			}
		}
		
		this.currentPrintJob = null;
		this.gcodeProcessing = "";
		this.gCodeToSend = "";
		this.squarePixelSize = 10;
		var printerName = $location.search().printerName;
		$http.get("/services/printers/get/" + printerName).success(
	        		function (data) {
	        			controller.currentPrinter = data;
	        		})
		$http.get("/services/printJobs/getByPrinterName/" + printerName).success(
        		function (data) {
        			controller.currentPrintJob = data;
        		})
        var gCodeSuccess = function (response) {
			if (response.data.response) {
				if (response.data.message.lastIndexOf("\n") != response.data.message.length-1) {
					response.data.message += "\n";
				}
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
        this.executeGCode = function executeGCode() {
			$http.get("services/printers/executeGCode/" + printerName + "/" + controller.gCodeToSend).then(gCodeSuccess, errorFunction)
		}
        this.projector = function projector(startStop) {
			$http.get("services/printers/" + startStop + "Projector/" + printerName).then(gCodeSuccess, errorFunction)
		}
        this.showCalibrationScreen = function showCalibrationScreen() {
			$http.get("services/printers/showCalibrationScreen/" + printerName + "/" + controller.squarePixelSize).then(gCodeSuccess, errorFunction)
		}
        this.showBlankScreen = function showBlankScreen() {
			$http.get("services/printers/showBlankScreen/" + printerName).then(gCodeSuccess, errorFunction)
		}
        this.overrideExposureTime = function overrideExposureTime() {
			$http.get("services/printJobs/overrideExposuretime/" + controller.currentPrintJob.id + "/" + controller.currentPrintJob.exposureTime).then(gCodeSuccess, errorFunction)
		}
        this.overrideLiftDistance = function overrideLiftDistance() {
			$http.get("services/printJobs/overrideZLiftDistance/" + controller.currentPrintJob.id + "/" + controller.currentPrintJob.zliftDistance).then(gCodeSuccess, errorFunction)
		}
        this.overrideLiftSpeed = function overrideLiftSpeed() {
			$http.get("services/printJobs/overrideZLiftSpeed/" + controller.currentPrintJob.id + "/" + controller.currentPrintJob.zliftSpeed).then(gCodeSuccess, errorFunction)
		}
        this.shutter = function shutter(shutterState) {
			$http.get("services/printers/" + shutterState + "shutter/" + printerName).then(gCodeSuccess, errorFunction)
		}

		attachToPrinter(printerName);
	}])

})();