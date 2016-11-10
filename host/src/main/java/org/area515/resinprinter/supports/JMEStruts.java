package org.area515.resinprinter.supports;

import java.io.FileInputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.area515.resinprinter.slice.StlFile;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.util.BufferUtils;

public class JMEStruts {
	private static class PolyFile extends StlFile<Mesh, Vector3f> {
		private List<Vector3f> vertex = new ArrayList<>();
		private List<Vector3f> normals = new ArrayList<>();
		
		@Override
		protected void buildTriangle(Vector3f p1, Vector3f p2, Vector3f p3, double[] normal) {
			vertex.add(p1);
			vertex.add(p2);
			vertex.add(p3);
			normals.add(new Vector3f((float)normal[0], (float)normal[1], (float)normal[2]));
		}

		@Override
		protected Vector3f buildPoint(double x, double y, double z) {
			return new Vector3f((float)x, (float)y, (float)z);
		}
		
		@Override
		protected Collection<Mesh> createSet() {
			return new ArrayList<Mesh>();
		}

		@Override
		protected Mesh getFirstTriangle() {
	        Mesh mesh = new Mesh();
	        mesh.setMode(Mode.Triangles);
	        
            IntBuffer ib = BufferUtils.createIntBuffer(normals.size() * 3);
            mesh.setBuffer(VertexBuffer.Type.Index, 3, ib);
            IndexIntBuffer indexBuf = new IndexIntBuffer(ib);
            
            FloatBuffer normBuf = BufferUtils.createFloatBuffer(vertex.size() * 3);
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
	        
            FloatBuffer posBuf  = BufferUtils.createFloatBuffer(vertex.size() * 3);
            mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
            
            normBuf.position(0);
            posBuf.position(0);

            for (int i = 0; i < normals.size(); i++){
                int index = i * 3; // current face * 3 = current index
                
                posBuf.put(vertex.get(index).x).put(vertex.get(index).y).put(vertex.get(index).z);
                posBuf.put(vertex.get(index+1).x).put(vertex.get(index+1).y).put(vertex.get(index+1).z);
                posBuf.put(vertex.get(index+2).x).put(vertex.get(index+2).y).put(vertex.get(index+2).z);

                normBuf.put(normals.get(i).x).put(normals.get(i).y).put(normals.get(i).z);
                normBuf.put(normals.get(i).x).put(normals.get(i).y).put(normals.get(i).z);
                normBuf.put(normals.get(i).x).put(normals.get(i).y).put(normals.get(i).z);

                indexBuf.put(index,   index);
                indexBuf.put(index+1, index+1);
                indexBuf.put(index+2, index+2);
            }

            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();
			return mesh;
		}
	};
	
	public static void main(String[] args) throws Exception {
		PolyFile figure = new PolyFile();
		long load = System.currentTimeMillis();
		//figure.load(StlFile.class.getResourceAsStream("lenscap-36mm.stl"), true);
		if (args.length == 1) {
			figure.load(new FileInputStream(args[0]), true);
		} else {
			figure.load(new FileInputStream("C:\\Users\\wgilster\\uploaddir\\Fat_Guy_Statue.stl"), true);
		}
		//figure.load(StlFile.class.getResourceAsStream("Homebrew_Finds_Magnet_Mounting_Thingy.stl"), true);
		long buildMesh = System.currentTimeMillis();
		System.out.println("loading:" + (buildMesh - load));
		Mesh polys = figure.getFirstTriangle();
		long creatingCollisionData = System.currentTimeMillis();
		System.out.println("buildMesh:" + (creatingCollisionData - buildMesh));
		polys.createCollisionData();
		System.out.println("creatingCollisionData:" + (System.currentTimeMillis() - creatingCollisionData));
	}
}
