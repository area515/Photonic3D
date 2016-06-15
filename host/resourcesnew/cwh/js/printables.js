(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintablesController", ['$scope', '$http', '$location', '$uibModal', '$anchorScroll', 'cwhWebSocket', function ($scope, $http, $location, $uibModal, $anchorScroll, cwhWebSocket) {
		controller = this;
		
		this.currentPrintable = null;
		this.supportedFileTypes = null;
		// Code added by Wilbur Shi
		this.flipped = {
			value: false
		};

		if (this.currentPrintable != null) {
			// Do flipping stuff
			this.testName = currentPrintable.name;
		} else {
			// Hide preview area in the html (checkbox and preview panel)
			this.testName = null;
		}
		// End code added by Wilbur Shi
		this.refreshPrintables = function refreshPrintables() {
			$http.get("/services/printables/list").success(
        		function (data) {
        			controller.printables = data;
        		}
	        );
		}
		this.hostSocket = cwhWebSocket.connect("services/hostNotification", $scope).onJsonContent(
			function(data) {
				if (data.notificationEvent == "FileUploadComplete") {
					controller.refreshPrintables();
				}
			}
		);
		if (this.hostSocket == null) {
			$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
		}
		this.printPrintable = function printPrintable() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);
	        $http.post("/services/printables/print/" + printableName + "." + printableExtension).success(
	        		function (data) {
	        			controller.refreshPrintables();
	        			//$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrintables, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
		}
		this.deletePrintable = function deletePrintable() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);
	        $http.delete("/services/printables/delete/" + printableName + "." + printableExtension).success(
	        		function (data) {
	        			controller.refreshPrintables();
	        			controller.currentPrintable = null;
	        			//$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
	    }
		this.changeCurrentPrintable = function changeCurrentPrintable(newPrintable) {
			controller.currentPrintable = newPrintable;
		}

		//TODO: When we get an upload complete message, we need to refresh file list...
		this.showUpload = function showUpload() {
			var fileChosenModal = $uibModal.open({
		        animation: true,
		        templateUrl: 'upload.html',
		        controller: 'UploadFileController',
		        size: "lg",
		        resolve: {
		        	title: function () {return "Upload Printable";},
		        	supportedFileTypes: function () {return null},
		        	getRestfulFileUploadURL: function () {return function (filename) {return '/services/printables/uploadPrintableFile/' + encodeURIComponent(filename);}},
		        	getRestfulURLUploadURL: function () {return function (filename, url) {return "services/printables/uploadviaurl/" + encodeURIComponent(filename) + "/" + encodeURIComponent(url)}}
		        }
			});
			
			//fileChosenModal.result.then(function (savedPrinter) {$scope.savePrinter(savedPrinter, newPrinter)});
		}
		
		this.getPrintableIconClass = function getPrintableIconClass(printable) {
			if (printable.printFileProcessor.friendlyName === 'Image') {
				return "fa-photo";
			}
			if (printable.printFileProcessor.friendlyName === 'Maze Cube') {
				return "fa-cube";
			}			
			if (printable.printFileProcessor.friendlyName === 'STL 3D Model') {
				return "fa-object-ungroup";
			}			
			if (printable.printFileProcessor.friendlyName === 'Creation Workshop Scene') {
				return "fa-diamond";
			}
			if (printable.printFileProcessor.friendlyName === 'Zip of Slice Images') {
				return "fa-stack-overflow";
			}
			if (printable.printFileProcessor.friendlyName === 'Simple Text') {
				return "fa-bold";
			}
			return "fa-question-circle";
		}

		this.refreshPrintables();
	}])

})();
