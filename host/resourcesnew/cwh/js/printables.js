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

		this.projectImage = false;

		this.handlePreviewError = function handlePreviewError() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);				
			$http.get("/services/customizers/renderFirstSliceImage/" + printableName + "." + printableExtension + "?projectImage=" + controller.projectImage).success(
				function (data) {

				}).error(
				function (data, status, headers, config, statusText) {
					// $scope.$emit("HTTPError", {status:status, statusText:data});
					controller.errorMsg = "Error: " + data;
					controller.showControls = false;
				});
		}

		this.createTemplateCustomizer = function createTemplateCustomizer(printable) {
			var customizerName = printable.name + "." + printable.extension;
			var customizer = {
				name: customizerName,
				printerName: printable.printerName,
				printableName: printable.name,
				printableExtension: printable.extension,
				supportsAffineTransformSettings: true,
				affineTransformSettings: {
					// affineTransformScriptCalculator: null,
					xscale: 1,
					yscale: 1,
					xtranslate: 0,
					ytranslate: 0
				}
			};
			return customizer;
		}

		this.findCurrentCustomizer = function findCurrentCustomizer(printable) {
			if (printable === null) {
				return null;
			}
			var customizer = controller.customizers[printable.name + "." + printable.extension];
			if (customizer === null || customizer === undefined) {
				customizer = controller.createTemplateCustomizer(printable);
				controller.customizers[printable.name + "." + printable.extension] = customizer;
			} 
			return customizer;
		}

		this.setPreview = function setPreview(reload) {
			// var parameter = controller.currentCustomizer;
			// console.log(parameter);
			if (controller.currentPrintable !== null && controller.currentPrintable !== undefined) {
				var printableName = encodeURIComponent(controller.currentPrintable.name);
				var printableExtension = encodeURIComponent(controller.currentPrintable.extension);	

				var parameter = controller.findCurrentCustomizer(controller.currentPrintable);	
				// do things with the currentCustomizer and get the png then set a variable like currentPreview to that png so that HTML page can display it
				$http.post("/services/customizers/upsertCustomizer", parameter).success(
					function (data) {
						controller.currentPreviewImg = "/services/customizers/renderFirstSliceImage/" + printableName + "." + printableExtension + "?projectImage=" + controller.projectImage;
						if (reload) {
							controller.currentPreviewImg += '&decache=' + Math.random();
						}
						// console.log("reached success while rendering first slice image, browser side");
					}).error(
	    				function (data, status, headers, config, statusText) {
	    					// console.log("up in here set preview failure");
	 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
		        		});

			}
		};

		this.changeCurrentPrintable = function changeCurrentPrintable(newPrintable) {
			controller.currentPrintable = newPrintable;
			controller.errorMsg = "";
			this.showControls = true;
			this.projectImage = false;
			this.setPreview(true);		
		};

		this.refreshPrintables = function refreshPrintables() {
	  		$http.get("services/printables/list").success(
	  			function (data) {
	  				controller.printables = data;
	  				$http.get("services/customizers/list").success(
	  					function (data) {
	  						controller.customizers = data;
	  						var firstPrintable = controller.printables[0];
	  						if (firstPrintable === undefined || firstPrintable === null) {
	  							this.showControls = false;
                 			} else {
                 				if (controller.currentPrintable === undefined || controller.currentPrintable === null) {
                 					controller.changeCurrentPrintable(firstPrintable); 		
                 				}
                 			}
	  					});
	  			})
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
			var currentCustomizer = controller.findCurrentCustomizer(controller.currentPrintable);
			if (currentCustomizer !== null) {
				//customizer returns a json object. js side only knows api
				var affineTransformSettings = currentCustomizer.affineTransformSettings;
				affineTransformSettings.yscale = y;
			}
			this.setPreview(true);
		}

		this.setProjectImage = function setProjectImage(projectImage) {
			controller.projectImage = projectImage;
			controller.setPreview(true);
		}

		this.isProjectImage = function isProjectImage() {
			return !(controller.projectImage);
		}

		this.isFlipped = function isFlipped() {
			if (controller.currentPrintable !== null) {
			var currentCustomizer = controller.findCurrentCustomizer(controller.currentPrintable);
			var affineTransformSettings = currentCustomizer.affineTransformSettings;
			if (affineTransformSettings.yscale == -1) {
				return true;
			}
			return false;				
			}

		}

		this.resetTranslation = function resetTranslation() {
			var currentCustomizer = controller.findCurrentCustomizer(controller.currentPrintable);
			var affineTransformSettings = currentCustomizer.affineTransformSettings;
			affineTransformSettings.xtranslate = 0;
			affineTransformSettings.ytranslate = 0;
			this.setPreview(true);
		}

		this.isNotModified = function isNotModified() {
			if (controller.currentPrintable !== null) {
			var currentCustomizer = controller.findCurrentCustomizer(controller.currentPrintable);
			var affineTransformSettings = currentCustomizer.affineTransformSettings;
			if (affineTransformSettings.xtranslate !== 0 || affineTransformSettings.ytranslate !== 0) {
				return false;
			}					
			return true;				
			}

		}

		this.goToSlacer = function goToSlacer() {
			window.open("/slacer", "slacer");
		}

		this.changeTranslate = function changeTranslate(x, y) {
			var currentCustomizer = controller.findCurrentCustomizer(controller.currentPrintable);
			var affineTransformSettings = currentCustomizer.affineTransformSettings;

			affineTransformSettings.xtranslate += affineTransformSettings.xscale * x;
			affineTransformSettings.ytranslate += affineTransformSettings.yscale * y;
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
