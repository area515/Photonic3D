package org.area515.resinprinter.stl;
import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.IOException;

/**
 * Title:        STL Loader
 * Description:  STL files loader (Supports ASCII and binary files) for Java3D
 *               Needs JDK 1.4 due to endian problems
 * Copyright:    Copyright (c) 2001
 * Company:      Universidad del Pais Vasco (UPV/EHU)
 * @author:      Carlos Pedrinaci Godoy
 * @version 1.0
 *
 *
 *   STL FILE PARSER:
 *   Extends StreamTokenizer
 *   For Ascii files
 *   Each token returned is checked in StlFile.java
 *   Format of an ASCII STL file:
 *
 *     solid /users/vis/dru/wedge.stl
 *       facet normal -1 0 0
 *         outer loop
 *           vertex 0.005 1 0
 *           vertex 0 0.543 0
 *           vertex 0.453 1 1
 *         endloop
 *       endfacet
 *            .
 *            .
 *            .
 *     endsolid /users/vis/dru/wedge.stl
 *
 *  That Class is necessary because scientific numbers are not correctly readed by Tokenizer
 *  we must then extend that class and define another getNumber
 */

public class StlFileParser extends StreamTokenizer
{
  /**
   * Constructor: object creation and setup
   *
   * @param r The Reader instance
   */
  public StlFileParser(Reader r)
  {
    super(r);
    setup();
  }

  /**
   * Method that sets some params of the Tokenizer for reading the file correctly
   */
  public void setup()
  {
    resetSyntax();
    eolIsSignificant(true);   // The End Of Line is important
    lowerCaseMode(true);

    // All printable ascii characters
    wordChars('!', '~');

    whitespaceChars(' ', ' ');
    whitespaceChars('\n', '\n');
    whitespaceChars('\r', '\r');
    whitespaceChars('\t', '\t');
  }// End setup

  /**
   * Gets a number from the stream.  Note that we don't recognize
   * numbers in the tokenizer automatically because numbers might be in
   * scientific notation, which isn't processed correctly by
   * StreamTokenizer.  The number is returned in nval.
   *
   * @return boolean.
   */
  boolean getNumber()
  {
    int t;

    try {
        nextToken();
	if (ttype != TT_WORD)
	  throw new IOException("Expected number on line " + lineno());
	nval =  (Double.valueOf(sval)).doubleValue();
    }
    catch (IOException e) {
      System.err.println(e.getMessage());
      return false;
    }
    catch (NumberFormatException e) {
      System.err.println(e.getMessage());
      return false;
    }
    return true;
  } // end of getNumber

}// End of StlFileParser