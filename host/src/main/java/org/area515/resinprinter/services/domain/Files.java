package org.area515.resinprinter.services.domain;

import java.util.ArrayList;


public class Files {
	ArrayList<String> names;
	public Files(){this.names = new ArrayList<String>();}
	public Files(ArrayList<String> names){this.names = new ArrayList<String>();this.names = names;}
	public ArrayList<String> getNames(){return names;}
	public void setNames(ArrayList<String> names){this.names = names;}
}