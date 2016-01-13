angular.module('cwh.comport', []).directive('cwhComport', ['$window', '$animate', function($window, $animate) {
    return {
        restrict: 'AE', //Use as attribute and element
		scope: { ngModel:"=", serialportchoices: '=', speedchoices: '=', paritychoices: "=", databitschoices: "=", stopbitschoices: "=", serialportlabel: "="},
        template: 	'<div class="form-group">' +
        			'	<div class="col-xs-4 ">' + 
					'		<label class="col-xs-11 control-label">{{serialportlabel}}</label>' + 
					'		<input class="col-xs-1 control-label btn" type="checkbox" name="advanced" value="advanced" ng-model="showAdvancedOptions">' +
					'	</div>' + 
					'	<div class="col-xs-8">' + 
					'		<select name="type" class="form-control" ng-model="ngModel.PortName" ng-options="serial for serial in serialportchoices">' +
					'			<option value="">Select a Serial Port...</option>' + 
					'		</select>' +
					'	</div>' + 
					'</div>' + 
					'<div class="form-group" ng-show="showAdvancedOptions" >' +
					'	<label class="col-xs-4 control-label">Speed</label>' + 
					'	<div class="col-xs-8">' + 
					'		<select name="type" class="form-control" ng-model="ngModel.Speed" ng-options="speed for speed in speedchoices">' +
					'			<option value="">Select a Speed...</option>' + 
					'		</select>' +
					'	</div>' +
					'</div>' +
					'<div class="form-group" ng-show="showAdvancedOptions" >' +
					'	<label class="col-xs-4 control-label">Databits</label>' + 
					'	<div class="col-xs-8">' + 
					'		<select name="type" class="form-control" ng-show="showAdvancedOptions" ng-model="ngModel.Databits" ng-options="data for data in databitschoices">' +
					'			<option value="">Select Databits...</option>' + 
					'		</select>' +
					'	</div>' +
					'</div>' +
					'<div class="form-group" ng-show="showAdvancedOptions" >' +
					'	<label class="col-xs-4 control-label">Parity</label>' + 
					'	<div class="col-xs-8">' + 
					'		<select name="type" class="form-control" ng-show="showAdvancedOptions" ng-model="ngModel.Parity" ng-options="parity for parity in paritychoices">' +
					'			<option value="">Select Parity...</option>' + 
					'		</select>' +
					'	</div>' +
					'</div>' +
					'<div class="form-group" ng-show="showAdvancedOptions" >' +
					'	<label class="col-xs-4 control-label">Stopbits</label>' + 
					'	<div class="col-xs-8">' + 
					'		<select name="type" class="form-control" ng-show="showAdvancedOptions" ng-model="ngModel.Stopbits" ng-options="stop for stop in stopbitschoices">' +
					'			<option value="">Select Stopbits...</option>' + 
					'		</select>' +
					'	</div>' +
					'</div>',
        //require: 'ngModel',
        link: function(scope, iElement, iAttrs, ngModelController) {
        }
    }
}]);