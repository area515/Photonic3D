(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("EditResinController", function ($scope, $http, $uibModalInstance, title) {
		
		// init a new resin profile
		$scope.title = title;
		$scope.inkConfigArr = { 
				FirstLayerTime:'', 
				LayerTime:'', 
				Name:'', 
				NumberofBottomLayers:'', 
				PercentageOfPrintMaterialConsideredEmpty:'', 
				ResinPriceL:"", SliceHeight:'' 
				};
		
		$scope.save = function () {
			console.log($scope.inkConfigArr);
			$uibModalInstance.close($scope.inkConfigArr);
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
		
	})
	
})();