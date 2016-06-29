(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintablesController", ['$scope', '$http', '$location', '$uibModal', '$anchorScroll', 'cwhWebSocket', function ($scope, $http, $location, $uibModal, $anchorScroll, cwhWebSocket) {
		controller = this;
		
		this.currentPrintable = null;
		this.currentCustomizer = null;
		this.supportedFileTypes = null;

		// Possibly have this to save the currentPreview png image
		this.currentPreviewImg = null;

	        
	    // Code added by Wilbur Shi
		this.customizers = {};

		this.test = function test() {
			$http.post("/services/customizers/customizerTest", controller.currentCustomizer).success(
				function (data) {
					console.log(data);
				}
			).error(
				function (data) {
					console.log("error");
				});
			// console.log("Hi this is a test");
		};

		this.setPreview = function setPreview() {
			var parameter = controller.currentCustomizer;
			// do things with the currentCustomizer and get the png then set a variable like currentPreview to that png so that HTML page can display it
			$http.post("/services/customizers/upsertCustomizer", parameter).success(
				function (data) {
					// console.log("reached success while rendering first slice image, browser side");
				}).error(
				function (data) {
					// error stuff
					// console.log("error while trying rendering first slice image, browser side");
				});
			controller.currentPreviewImg = "/services/customizers/renderFirstSliceImage/" + controller.currentPrintable.name;
		};

		this.refreshPrintables = function refreshPrintables() {
			$http.get("/services/printables/list").success(
        		function (data) {
        			controller.printables = data;
					var length = controller.printables.length;
					for (var i = 0; i < length; i++) {
						var currPrint = controller.printables[i];
						var currName = currPrint.name;
						if (!(currName in controller.customizers)) {						  
						    // console.log("we do not have customizer for name: " + currName);										
							var customizer = {
								name: currName,
								printerName: currPrint.printerName,
								printableName: currPrint.name,
								printableExtension: currPrint.extension,
								supportsAffineTransformSettings: true,
								affineTransformSettings: {
									affineTransformScriptCalculator: "placeholder",
									xscale: 0,
									yscale: 0,
									xtranslate: 0,
									ytranslate: 0
								}
							};
						controller.customizers[currName] = customizer;
						// console.log("we have customizer for " + controller.customizers.currName.name);
						}				
					}
					controller.currentPrintable = controller.printables[0];
					controller.currentCustomizer = controller.customizers[controller.currentPrintable.name];
					controller.setPreview();
        		}
	        );
	        // End code added by Wilbur Shi
		};
		this.hostSocket = cwhWebSocket.connect("services/hostNotification", $scope).onJsonContent(
			function(data) {
				if (data.notificationEvent == "FileUploadComplete") {
					controller.refreshPrintables();
				}
			}
			// potentially add one for when customizerischanged to reset the preview again (this is the whole preview/customizer thing goes into a separate controller or somethng like that)
		);
		if (this.hostSocket === null) {
			$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
		}

		this.changeFlip = function changeFlip() {
			if (controller.currentCustomizer !== null) {
				//customizer returns a json object. js side only knows api
				controller.changeMsg = controller.currentCustomizer.name + " yscale is ";
				var affineTransformSettings = controller.currentCustomizer.affineTransformSettings;
				if (affineTransformSettings.yscale) {
					// affineTransformSettings.yscale = -1;
					controller.changeMsg += "-1";
				} else {
					// affineTransformSettings.yscale = 0;
					controller.changeMsg += "0";
				}
			}
			this.setPreview();
		};
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
		};
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
	        delete controller.customizers[printableName];
	        this.refreshPrintables();
	    };
		this.changeCurrentPrintable = function changeCurrentPrintable(newPrintable) {
			controller.currentPrintable = newPrintable;
			// Code added by Wilbur Shi
			var currName = newPrintable.name;
			// console.log(currName);
			controller.currentCustomizer = controller.customizers[currName];
			this.setPreview();
			// // End code added by Wilbur Shi
		};

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
		        	getRestfulURLUploadURL: function () {return function (filename, url) {return "services/printables/uploadviaurl/" + encodeURIComponent(filename) + "/" + encodeURIComponent(url);}}
		        }
			});
			this.refreshPrintables();
			
			//fileChosenModal.result.then(function (savedPrinter) {$scope.savePrinter(savedPrinter, newPrinter)});
		};
		
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
		};

		this.refreshPrintables();
	}]);

})();
