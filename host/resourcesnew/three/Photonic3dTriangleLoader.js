THREE.Photonic3dTriangleLoader = function ( manager ) {
	this.manager = ( manager !== undefined ) ? manager : THREE.DefaultLoadingManager;
};

THREE.Photonic3dTriangleLoader.prototype = {
		
	constructor: THREE.Photonic3dTriangleLoader,
	
	load: function ( url, onLoad, onProgress, onError ) {
		var scope = this;
		var loader = new THREE.XHRLoader( scope.manager );
		loader.load( url, 
			function ( text ) {
				onLoad( scope.parse( JSON.parse( text ) ) );
			}, 
			onProgress, 
			onError 
		);
	},

	parse: function ( json ) {
		var geometry = new THREE.Geometry();
		json.forEach(function(item, index) {
			geometry.vertices.push( new THREE.Vector3( item.v[0], item.v[1], item.v[2]));
			geometry.vertices.push( new THREE.Vector3( item.v[3], item.v[4], item.v[5]));
			geometry.vertices.push( new THREE.Vector3( item.v[6], item.v[7], item.v[8]));
			
			length = geometry.vertices.length;
			var face = new THREE.Face3( length - 3, length - 2, length - 1, new THREE.Vector3( item.n[0], item.n[1], item.n[2] ) );
			face.color = new THREE.Color( 0xffffff );
			geometry.faces.push(face);
		});
		
		geometry.computeBoundingBox();
		geometry.computeBoundingSphere();
		
		return geometry;
	}
};