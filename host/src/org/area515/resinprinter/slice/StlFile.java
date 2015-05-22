package org.area515.resinprinter.slice;
/*
   Copyright 2001  Universidad del Pais Vasco (UPV/EHU)
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
// New from JDK 1.4 for endian related problems
import java.nio.ByteOrder;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * Title:         STL Loader
 * Description:   STL files loader (Supports ASCII and binary files) for Java3D
 *                Needs JDK 1.4 due to endian problems
 * 
 * Company:       Universidad del Pais Vasco (UPV/EHU)
 * @author:       Carlos Pedrinaci Godoy
 * 
 * Company:       National Aeronautics and Space Administration, Inlets and Nozzles (NASA/RTE0)
 * @author:       Avinash Devalla
 * 
 * @version:      1.0
 *
 * Contact : xenicp@yahoo.es
 *
 
 * 
 * Contact:	avinash.s.devalla@gmail.com
 *
 *
 * Things TO-DO:
 *    1.-We can't read binary files over the net.
 *    2.-For binary files if size is lower than expected (calculated with the number of faces)
 *    the program will block.
 *    3.-Improve the way for detecting the kind of stl file?
 *    Can give us problems if the comment of the binary file begins by "solid"
 */

public abstract class StlFile<T> {
  // Maximum length (in chars) of basePath
  private static final int MAX_PATH_LENGTH = 1024;

  // Global variables
  private int flag;                         // Needed cause implements Loader

  private File file;

  private boolean fromUrl = false;          // Usefull for binary files
  private boolean Ascii = true;             // File type Ascii -> true o binary -> false

  // Arrays with coordinates and normals
  // Needed for reading ASCII files because its size is unknown until the end
  //private ArrayList<Point3f> coordList;		// Holds Point3f
  //private ArrayList<Vector3f> normList;		// Holds Vector3f
  protected Set<T> triangles;
  protected double zmin = Double.MAX_VALUE;
  protected double zmax = -Double.MAX_VALUE;
  protected double xmin = Double.MAX_VALUE;
  protected double xmax = -Double.MAX_VALUE;
  protected double ymin = Double.MAX_VALUE;
  protected double ymax = -Double.MAX_VALUE;

  // GeometryInfo needs Arrays
  //private Point3f[] coordArray;
  //private Vector3f[] normArray;

  // Needed because TRIANGLE_STRIP_ARRAY
  // As the number of strips = the number of faces it's filled in objectToVectorArray
  //private int[] stripCounts;

  // Default = Not available
  private String objectName = new String("Not available");
  
  /**
  *  Constructor
  */
  public StlFile()
  {
  }

  /**
   * Method that reads the EOL
   * Needed for verifying that the file has a correct format
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readEOL(StlFileParser parser)
  {
    try
    {
    	parser.nextToken();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    if(parser.ttype != StlFileParser.TT_EOL)
    {
      System.err.println("Format Error:expecting End Of Line on line " + parser.lineno());
    }
  }

  /**
   * Method that reads the word "solid" and stores the object name.
   * It also detects what kind of file it is
   * TO-DO:
   *    1.- Better way control of exceptions?
   *    2.- Better way to decide between Ascii and Binary?
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readSolid(StlFileParser parser)
  {
    if(parser.sval == null || !parser.sval.equals("solid"))
    {
      //System.out.println("Expecting solid on line " + parser.lineno());
      // If the first word is not "solid" then we consider the file is binary
      // Can give us problems if the comment of the binary file begins by "solid"
      this.setAscii(false);
    }
    else  // It's an ASCII file
    {
      try
      {
          	parser.nextToken();
      }
      catch (IOException e)
      {
	  	System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
      }
      if( parser.ttype != StlFileParser.TT_WORD)
      {
	  	// Is the object name always provided???
	  	System.err.println("Format Error:expecting the object name on line " + parser.lineno());
      }
      else
      { 
	  	// Store the object Name
        this.setObjectName(new String(parser.sval));
        this.readEOL(parser);
      }
    }
  }//End of readSolid


  /**
   * Method that reads tokens and allows iteration through facet definition
   * Written by adevalla
   * 
   * @param parser - file parser, instance of StlFileParser
   * @param parseKey - string "outer", "loop", "endloop", or "endfacet"
   * */
  
  private void readToken(StlFileParser parser, String parseKey)
  {
		if (parser.ttype != StlFileParser.TT_WORD || !parser.sval.equals(parseKey))
	    {
			System.err.println("Format Error:expecting " + parseKey + " on line " + parser.lineno());
	    }
	    else 
	    {
	    	if (parseKey.equals("outer"))
	    	{
	    		try {
					parser.nextToken();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	readEOL(parser);
	    }
  }
  
  /**
   * Method that reads normal, vertex coordinates; stores them as Point3f.
   * Written by adevalla
   * 
   * @param parser - file parser, instance of StlFileParser
   * @param parseKey - string "normal", "vertex"
   * */
  
  private void read3d(StlFileParser parser, String parseKey)
  {
	  throw new IllegalArgumentException("Ascii file not supported");
	  /*
	  float x, y, z;

	    if(!(parser.ttype==StlFileParser.TT_WORD && parser.sval.equals(parseKey)))
	    {
	      System.err.println("Format Error:expecting '"+ parseKey +"' on line " + parser.lineno());
	    }
	    else
	    {
	      if (parser.getNumber())
	      {
		        x=(float)parser.nval;
		        if (parser.getNumber())
		        {
			          y=(float)parser.nval;
			          if (parser.getNumber())
			          {
			        	  z=(float)parser.nval;
			        	  // We add that vertex to the array of vertex
			        	  coordList.add(new Point3f(x, y, z));
			        	  readEOL(parser);
			          }
			          else System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
		        }
		        else System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
	      }
	      else System.err.println("Format Error: expecting coordinate on line " + parser.lineno());
	    }*/
  }
  
  /**
   * Method that reads a face of the object. 
   * (Cares about the format).
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readFacet(StlFileParser parser)
  {
	    if(parser.ttype != StlFileParser.TT_WORD || !parser.sval.equals("facet"))
	    {
	      System.err.println("Format Error:expecting 'facet' on line " + parser.lineno());
	    }
	    else
	    {
		      try
		      {
		          parser.nextToken();
		          read3d(parser, "normal");
		
		          parser.nextToken();
		          readToken(parser, "outer");
		
		          for (int i = 0; i < 3; i++) //reads three vertices
		          {
			          parser.nextToken();
			          read3d(parser, "vertex");
		          }
		
		          parser.nextToken();
		          readToken(parser, "endloop");
		
		          parser.nextToken();
		          readToken(parser, "endfacet");
		      }
		      catch (IOException e)
		      {
		        System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
		      }
	    }
  }// End of readFacet

  /**
   * Method that reads a face in binary files
   * All binary versions of the methods end by 'B'
   * As in binary files we can read the number of faces, we don't need
   * to use coordArray and normArray (reading binary files should be faster)
   *
   * @param in The ByteBuffer with the data of the object.
   * @param index The facet index
   *
   * @throws IOException
   */
  public abstract void readFacetB(ByteBuffer in, int index) throws IOException;
  
  /**
   * Method for reading binary files
   * Execution is completly different
   * It uses ByteBuffer for reading data and ByteOrder for retrieving the machine's endian
   * (Needs JDK 1.4)
   *
   * TO-DO:
   *  1.-Be able to read files over Internet
   *  2.-If the amount of data expected is bigger than what is on the file then
   *  the program will block forever
   *
   * @param file The name of the file
   *
   * @throws IOException
   */
  private void readBinaryFile(File file) throws IOException
  {
    FileInputStream data;                 // For reading the file
    ByteBuffer dataBuffer;                // For reading in the correct endian
    byte[] Info=new byte[80];             // Header data
    byte[] Array_number= new byte[4];     // Holds the number of faces
    byte[] Temp_Info;                     // Intermediate array

    int Number_faces; // First info (after the header) on the file

    // Get file's name
    if(fromUrl)
    {
      // FileInputStream can only read local files!?
      System.out.println("This version doesn't support reading binary files from internet");
    }
    else
    { // It's a local file
      data = new FileInputStream(file);

      // First 80 bytes aren't important
      if(80 != data.read(Info))
      { // File is incorrect
        //System.out.println("Format Error: 80 bytes expected");
        throw new IOException("STL Format Error: 80 bytes expected");
      }
      else
      { // We must first read the number of faces -> 4 bytes int
        // It depends on the endian so..

        data.read(Array_number);                      // We get the 4 bytes
        dataBuffer = ByteBuffer.wrap(Array_number);   // ByteBuffer for reading correctly the int
        dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
        Number_faces = dataBuffer.getInt();

        Temp_Info = new byte[50*Number_faces];        // Each face has 50 bytes of data

        data.read(Temp_Info);                         // We get the rest of the file

        dataBuffer = ByteBuffer.wrap(Temp_Info);      // Now we have all the data in this ByteBuffer
        dataBuffer.order(ByteOrder.nativeOrder());

        // We can create that array directly as we know how big it's going to be
        //coordArray = new Point3f[Number_faces*3]; // Each face has 3 vertex
        //normArray = new Vector3f[Number_faces];
        //stripCounts = new int[Number_faces];

        for(int i=0;i<Number_faces;i++)
        {
          //stripCounts[i]=3;
          try
          {
            readFacetB(dataBuffer,i);
            // After each facet there are 2 bytes without information
            // In the last iteration we dont have to skip those bytes..
            if(i != Number_faces - 1)
            {
              dataBuffer.get();
              dataBuffer.get();
            }
          }
          catch (IOException e)
          {
            // Quitar
            System.out.println("Format Error: iteration number " + i);
            throw new IOException("Format Error: iteration number " + i, e);
          }
        }//End for
      }// End file reading
    }// End else
  }// End of readBinaryFile

  /**
   * Method that reads ASCII files
   * Uses StlFileParser for correct reading and format checking
   * The beggining of that method is common to binary and ASCII files
   * We try to detect what king of file it is
   *
   * TO-DO:
   *  1.- Find a best way to decide what kind of file it is
   *  2.- Is that return (first catch) the best thing to do?
   *
   * @param parser The file parser. An instance of StlFileParser.
   */
  private void readFile(StlFileParser parser)
  {
    try{
        parser.nextToken();
        }
    catch (IOException e)
    {
      System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
      System.err.println("File seems to be empty");
      return;         // ????? Throw ?????
    }

    // Here we try to detect what kind of file it is (see readSolid)
    readSolid(parser);

    if(getAscii())
    { // Ascii file
      try
      {
          parser.nextToken();
      }
      catch (IOException e)
      {
       System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
      }

      // Read all the facets of the object
      while (parser.ttype != StlFileParser.TT_EOF && !parser.sval.equals("endsolid"))
      {
        readFacet(parser);
        try
        {
          parser.nextToken();
        }
        catch (IOException e)
        {
          System.err.println("IO Error on line " + parser.lineno() + ": " + e.getMessage());
        }
      }// End while

      // Why are we out of the while?: EOF or endsolid
      if(parser.ttype == StlFileParser.TT_EOF)
       System.err.println("Format Error:expecting 'endsolid', line " + parser.lineno());
    }//End of Ascii reading

    else
    { // Binary file
      try{
        readBinaryFile(file);
      }
      catch(IOException e)
      {
        System.err.println("Format Error: reading the binary file");
      }
    }// End of binary file
  }//End of readFile

  /**
   * The Stl File is loaded from the .stl file specified by
   * the filename.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param filename The name of the file with the object to load
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   * @throws IncorrectFormatException
   * @throws ParsingErrorException
   */
  public void load(File file) throws FileNotFoundException
  {
    //setBasePathFromFilename(filename);
    //setFileName(filename);     // For binary files
    this.file = file;
    Reader reader = new BufferedReader(new FileReader(file));
    
    load(reader);
  } // End of load(String)

   /**
   * The Stl file is loaded off of the web.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param url The url to load the onject from
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   * @throws IncorrectFormatException
   * @throws ParsingErrorException
   */
  /*public void load(URL url) throws FileNotFoundException
  {
    BufferedReader reader = null;
    setBaseUrlFromUrl(url);
    try 
    {
		reader = new BufferedReader(new InputStreamReader(url.openStream()));
	} 
    catch (IOException e) 
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    fromUrl = true;
    load(reader);
  } // End of load(URL)*/

  public abstract Set<T> createSet();
  /**
   * The Stl File is loaded from the already opened file.
   * To attach the model to your scene, call getSceneGroup() on
   * the Scene object passed back, and attach the returned
   * BranchGroup to your scene graph.  For an example, see
   * $J3D/programs/examples/ObjLoad/ObjLoad.java.
   *
   * @param reader The reader to read the object from
   *
   * @return Scene The scene with the object loaded.
   *
   * @throws FileNotFoundException
   * @throws IncorrectFormatException
   * @throws ParsingErrorException
   */
  private void load(Reader reader) throws FileNotFoundException
  {
    // That method calls the method that loads the file for real..
    // Even if the Stl format is not complicated I've decided to use
    // a parser as in the Obj's loader included in Java3D

    StlFileParser st = new StlFileParser(reader);

    // Initialize data
    //coordList = new ArrayList<Point3f>();
    //normList = new ArrayList<Vector3f>();
    triangles = createSet();//
    //triangles = new LinkedHashSet<Triangle3d>();//new TreeSet<Triangle3d>(new XYComparator());
    
    setAscii(true);      // Default ascii
    readFile(st);
    //makeScene();
  }
  
  /**
   * Method that creates the SceneBase with the stl file info
   *
   * @return SceneBase The scene
   */
  /*private void makeScene()
  {
    // Create Scene to pass back
    //SceneBase scene = new SceneBase();
    //BranchGroup group = new BranchGroup();
    //scene.setSceneGroup(group);

    // Store the scene info on a GeometryInfo
    //GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

    // Convert ArrayLists to arrays: only needed if file was not binary
    if(this.Ascii)
    {
    	coordArray = new Point3f[coordList.size()];
    	normArray = new Vector3f[normList.size()];
    	coordList.toArray(coordArray);
    	normList.toArray(normArray);
    	
    	//stripCounts = new int[coordArray.length / 3];
    	for (int i = 0; i < stripCounts.length; i++)
    	{
    		 stripCounts[i] = 3; //stripCounts holds the number of sides of each shape
    		 //since our stl file defines surfaces as a series of triangles, we're assigning it all as three.
    	}
    }

    //gi.setCoordinates(coordArray);
    //gi.setNormals(normArray);
    //gi.setStripCounts(stripCounts);
 

    // Put geometry into Shape3d
    //Shape3D shape = new Shape3D();
    //shape.setGeometry(gi.getGeometryArray());

    //group.addChild(shape);
    //scene.addNamedObject(objectName, shape);

    //return scene;
  } // end of makeScene*/

  
  
 
  /******************** Accessors and Modifiers ***************************/

  /*public URL getBaseUrl()
  {
    return baseUrl;
  }

  /**
   * Modifier for baseUrl, if accessing internet.
   *
   * @param url The new url
   */
  /*public void setBaseUrl(URL url)
  {
    baseUrl = url;
  }

 /* private void setBaseUrlFromUrl(URL url)
  {
    StringTokenizer stok =
      new StringTokenizer(url.toString(), "/\\", true);
    int tocount = stok.countTokens() - 1;
    StringBuffer sb = new StringBuffer(MAX_PATH_LENGTH);
    for(int i = 0; i < tocount ; i++) {
	String a = stok.nextToken();
	sb.append(a);*/
// 	if((i == 0) && (!a.equals("file:"))) {
// 	    sb.append(a);
// 	    sb.append(java.io.File.separator);
// 	    sb.append(java.io.File.separator);
// 	} else {
// 	    sb.append(a);
// 	    sb.append( java.io.File.separator );
// 	}
    /*}
    try {
      baseUrl = new URL(sb.toString());
    }
    catch (MalformedURLException e) {
      System.err.println("Error setting base URL: " + e.getMessage());
    }
  } // End of setBaseUrlFromUrl*/

  /*public String getBasePath()
  {
    return basePath;
  }*/

  /**
   * Set the path where files associated with this .stl file are
   * located.
   * Only needs to be called to set it to a different directory
   * from that containing the .stl file.
   *
   * @param pathName The new Path to the file
   */
  /*public void setBasePath(String pathName)
  {
    basePath = pathName;
    if (basePath == null || basePath == "")
	basePath = "." + java.io.File.separator;
    basePath = basePath.replace('/', java.io.File.separatorChar);
    basePath = basePath.replace('\\', java.io.File.separatorChar);
    if (!basePath.endsWith(java.io.File.separator))
	basePath = basePath + java.io.File.separator;
  } // End of setBasePath

  /**
   * Takes a file name and sets the base path to the directory
   * containing that file.
   */
  /*private void setBasePathFromFilename(String fileName)
  {
    // Get ready to parse the file name
    StringTokenizer stok =
      new StringTokenizer(fileName, java.io.File.separator);

    //  Get memory in which to put the path
    StringBuffer sb = new StringBuffer(MAX_PATH_LENGTH);

    // Check for initial slash
    if (fileName!= null && fileName.startsWith(java.io.File.separator))
      sb.append(java.io.File.separator);

    // Copy everything into path except the file name
    for(int i = stok.countTokens() - 1 ; i > 0 ; i--) {
      String a = stok.nextToken();
      sb.append(a);
      sb.append(java.io.File.separator);
    }
    setBasePath(sb.toString());
  } // End of setBasePathFromFilename*/

  public int getFlags()
  {
    return flag;
  }

  public void setFlags(int parm)
  {
    this.flag=parm;
  }

  public boolean getAscii()
  {
    return this.Ascii;
  }

  public void setAscii(boolean tipo)
  {
    this.Ascii = tipo;
  }

  /*public String getFileName()
  {
    return this.fileName;
  }

  public void setFileName(String filename)
  {
    this.fileName=new String(filename);
  }*/

  public String getObjectName()
  {
    return this.objectName;
  }

  public void setObjectName(String name)
  {
    this.objectName = name;
  }
	
	public Set<T> getTriangles() {
		return triangles;
	}
	
	public double getZmin() {
		return zmin;
	}
	
	public double getZmax() {
		return zmax;
	}
	
	public double getXmin() {
		return xmin;
	}
	
	public double getXmax() {
		return xmax;
	}
	
	public double getYmin() {
		return ymin;
	}
	
	public double getYmax() {
		return ymax;
	}

	public double getWidth() {
		return xmax - xmin;
	}

	public double getHeight() {
		return ymax - ymin;
	}
} // End of package stl_loader