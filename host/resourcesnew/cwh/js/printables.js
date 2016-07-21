(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintablesController", ['$scope', '$http', '$location', '$uibModal', '$anchorScroll', 'cwhWebSocket', function ($scope, $http, $location, $uibModal, $anchorScroll, cwhWebSocket) {
		controller = this;
		
		this.currentPrintable = null;
		this.currentCustomizer = null;
		this.supportedFileTypes = null;

		this.currentPreviewImg = null;
   
		this.customizers = {};
		this.errorMsg = "";

		this.handlePreviewError = function handlePreviewError() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);				
			$http.get("/services/customizers/renderFirstSliceImage/" + printableName + "." + printableExtension).success(
				function (data) {

				}).error(
				function (data, status, headers, config, statusText) {
					// $scope.$emit("HTTPError", {status:status, statusText:data});
					controller.errorMsg = "Error: " + data;
					controller.showControls = false;
				});
		}

		this.setPreview = function setPreview(reload) {
			var parameter = controller.currentCustomizer;
			// console.log(parameter);
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);			
			// do things with the currentCustomizer and get the png then set a variable like currentPreview to that png so that HTML page can display it
			$http.post("/services/customizers/upsertCustomizer", parameter).success(
				function (data) {
					controller.currentPreviewImg = "/services/customizers/renderFirstSliceImage/" + printableName + "." + printableExtension;
					if (reload) {
						controller.currentPreviewImg += '?decache=' + Math.random();
					}
					// console.log("reached success while rendering first slice image, browser side");
				}).error(
    				function (data, status, headers, config, statusText) {
    					console.log("up in here set preview failure");
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		});
		};

		this.changeCurrentPrintable = function changeCurrentPrintable(newPrintable) {
			controller.currentPrintable = newPrintable;
			var customizerName = newPrintable.name + "." + newPrintable.extension;

				// Set currentCustomizer to the customizer in the dictionary given the current printable name
			controller.currentCustomizer = controller.customizers[customizerName];
			controller.errorMsg = "";
			this.showControls = true;
			this.setPreview(true);		
		};

		this.refreshPrintables = function refreshPrintables() {
			$http.get("/services/printables/list").success(
        		function (data) {
        			controller.printables = data;    			
        			$http.get("services/customizers/list").success(
        				function (data) {     					
        					controller.customizers = data;
                 			controller.initializeCustomizers(function () { 
                 				var firstPrintable = controller.printables[0];
                 				if (firstPrintable !== undefined && firstPrintable !== null) {
                 					if (controller.currentPrintable === undefined || controller.currentPrintable === null) {
                 						controller.changeCurrentPrintable(firstPrintable); 		
                 					}
                 				}
                 			});           							
        				});

        		}
	        );
		};

		this.initializeCustomizers = function initializeCustomizers(callback) {
			var length = controller.printables.length;
			console.log(controller.printables);
			for (var i = 0; i < length; i++) {
				var currPrint = controller.printables[i];
				var customizerName = currPrint.name + "." + currPrint.extension;
				if (!(customizerName in controller.customizers)) {		
					console.log("Here is the customizer name that is not in controller.customizers " + customizerName);				  
				    // console.log("we do not have customizer for name: " + customizer.name);										
					var customizer = {
						name: customizerName,
						printerName: currPrint.printerName,
						printableName: currPrint.name,
						printableExtension: currPrint.extension,
						supportsAffineTransformSettings: true,
						affineTransformSettings: {
							// affineTransformScriptCalculator: null,
							xscale: 1,
							yscale: 1,
							xtranslate: 0,
							ytranslate: 0
						}
					};
					$http.post("/services/customizers/upsertCustomizer", customizer).success(
						function (data) {								
					}).error(
    					function (data, status, headers, config, statusText) {					
 	        				$scope.$emit("HTTPError", {status:status, statusText:data});

	        			});
					controller.customizers[customizerName] = customizer;
				}				
			}		
			callback();

			// Alternate approach by forcing the synchronous calls to be async (having a function that calls itself after the http request completes). We do this because we set the dictionary
			// values after the http request is completed. in the prior for-loop version, we take advantage of the synchronous call by setting dictionary values outside of the http request success callback.

						// var length = controller.printables.length;
			// var loop = function(counter, currPrint) {
			// 	if (counter === undefined) {
			// 		counter = 0;
			// 	}
			// 	if (counter >= length) {
			// 		callback();			
			// 		return;
			// 	}
			// 	if (currPrint === undefined || currPrint === null) {
			// 		return;
			// 	}

			// 	var customizerName = currPrint.name + "." + currPrint.extension;
			// 	// console.log(controller.customizers);
			// 	if (!(customizerName in controller.customizers)) {											
			// 		var customizer = {
			// 			name: customizerName,
			// 			printerName: currPrint.printerName,
			// 			printableName: currPrint.name,
			// 			printableExtension: currPrint.extension,
			// 			supportsAffineTransformSettings: true,
			// 			affineTransformSettings: {
			// 				// affineTransformScriptCalculator: null,
			// 				xscale: 1,
			// 				yscale: 1,
			// 				xtranslate: 0,
			// 				ytranslate: 0
			// 			}
			// 		};
			// 		$http.post("/services/customizers/upsertCustomizer", customizer).success(
			// 			function (data) {
			// 				controller.customizers[customizer.name] = customizer;	
			// 				counter ++;
			// 				loop(counter, controller.printables[counter]);								
			// 		}).error(
   //  					function (data, status, headers, config, statusText) {					
 	 //        				$scope.$emit("HTTPError", {status:status, statusText:data});

	  //       			});
			// 	} else {
			// 		counter ++;
			// 		loop(counter, controller.printables[counter]);
			// 	}
			// };
			// loop(0, controller.printables[0]);
		}

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

		this.printWithCustomizer = function printWithCustomizer() {
			// TODO: API Call to CustomizerService.print() and handle printing w/ customizer
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);
	        $http.post("/services/customizers/print/" + printableName + "." + printableExtension).success(
	        		function (data) {
	        			//controller.refreshPrintables();
	        			//$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrintables, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
		}

		this.changeFlip = function changeFlip(y) {
			if (controller.currentCustomizer !== null) {
				//customizer returns a json object. js side only knows api
				controller.changeMsg = controller.currentCustomizer.name + " yscale is ";
				var affineTransformSettings = controller.currentCustomizer.affineTransformSettings;
				affineTransformSettings.yscale = y;
				// if (affineTransformSettings.yscale ) {
				controller.changeMsg = affineTransformSettings.yscale;
				// } else {
				// 	controller.changeMsg += "1";
				// }
				// console.log(affineTransformSettings);
			}
			this.setPreview(true);
		}

		this.isFlipped = function isFlipped() {
			// console.log(controller.currentCustomizer);
			if (controller.currentCustomizer !== null && controller.currentCustomizer !== undefined) {
				var affineTransformSettings = controller.currentCustomizer.affineTransformSettings;
				if (affineTransformSettings.yscale == -1) {
					return true;
				}
			}
			return false;
		}

		this.isNotFlipped = function isNotFlipped() {
			return !controller.isFlipped(); 
		}

		this.resetTranslation = function resetTranslation() {
			if (controller.currentCustomizer !== null) {
				var affineTransformSettings = controller.currentCustomizer.affineTransformSettings;
				affineTransformSettings.xtranslate = 0;
				affineTransformSettings.ytranslate = 0;
			}
			this.setPreview(true);
		}

		this.isNotModified = function isNotModified() {
			if (controller.currentCustomizer !== null && controller.currentCustomizer !== undefined) {
				var affineTransformSettings = controller.currentCustomizer.affineTransformSettings;
				if (affineTransformSettings.xtranslate !== 0 || affineTransformSettings.ytranslate !== 0) {
					return false;
				}					

			}
			return true;
		}

		this.changeTranslate = function changeTranslate(x, y) {
			if (controller.currentCustomizer !== null) {
				var affineTransformSettings = controller.currentCustomizer.affineTransformSettings;
				if (affineTransformSettings.yscale === -1) {
					y = -y;
				}
				affineTransformSettings.xtranslate += x;
				affineTransformSettings.ytranslate += y;
			}
			this.setPreview(true);
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
		};
		this.deletePrintable = function deletePrintable() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);
			var fileName = printableName + "." + printableExtension;
	        $http.delete("/services/printables/delete/" + fileName).success(
	        		function (data) {
	        			$http.delete("services/customizers/removeCustomizer/" + fileName).success(
	        				function (data) {
	        			        delete controller.customizers[printableName + "." + printableExtension];			
	        				    controller.refreshPrintables();
	        					controller.currentPrintable = null;
	        				}).error(
	        				function (data, status, headers, config, statusText) {
	        					$scope.$emit("HTTPError", {status:status, statusText:data});
	        				})
	        			//$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})


	        this.refreshPrintables();
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

	cwhApp.directive('handleError', function() {
			return {
				link: function(scope, element, attrs) {
					
					var pc = scope.printablesController;

					element.bind('error', function() {
						pc.handlePreviewError();
						scope.$apply();
					});

				}
			};
		});


})();
