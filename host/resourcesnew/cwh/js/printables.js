(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintablesController", ['$scope', '$http', '$location', '$uibModal', '$anchorScroll', 'cwhWebSocket', function ($scope, $http, $location, $uibModal, $anchorScroll, cwhWebSocket) {
		controller = this;
		
		this.currentPrintable = null;
		this.currentCustomizer = null;
		this.supportedFileTypes = null;

		this.refreshPrintables = function refreshPrintables() {
			$http.get("/services/printables/list").success(
        		function (data) {
        			controller.printables = data;
        		}
	        );
	        // Code added by Wilbur Shi
	        $http.get("/services/customizers/list").success(
        		function (data) {
        			controller.customizers = data;
        		}
	        );
	        // End code added by Wilbur Shi
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

		// Code added by Wilbur Shi
		// this.flipped = {
		// 	value: false
		// };

		// if (this.currentPrintable != null) {
		// 	// Do flipping stuff
		// 	this.testName = currentPrintable.name;
		// } else {
		// 	// Hide preview area in the html (checkbox and preview panel)
		// 	this.testName = null;  
		// }
		this.changeFlip = function changeFlip(flip) {
			if (controller.currentCustomizer != null) {
				var affineTransformSettings = controller.currentCustomizer.getAffineTransformSettings();
				if (flip) {
					affineTransformSettings.setyScale(-1);
					controller.changeMsg = "Set yScale to -1";
				} else {
					affineTransformSettings.setyScale(0);
					controller.changeMsg = "Set yScale to 0";
				}
			}

			// Since there is no currentCustomizer, here is a placeholder msg: 
			controller.changeMsg = "CustomizerService is not implemented, so here is a placeholder changeMsg: ";
			if (flip) {
				controller.changeMsg += "Set yScale to -1.";
			} else {
				controller.changeMsg += "Set yScale to 0.";
			}
		}
		// End code added by Wilbur Shi

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
			// Code added by Wilbur Shi
			$http.get("/services/customizers/getByPrintableName/" + newPrintable.name).success(
									function (data) {
										// Once this method is implemented, change null to data in order to save the correct Customizer.
										controller.currentCustomizer = null;
									});
			// End code added by Wilbur Shi
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
