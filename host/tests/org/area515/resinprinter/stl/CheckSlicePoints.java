package org.area515.resinprinter.stl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

public class CheckSlicePoints {
	public static void copyFileToPackage(File sourceFile) throws URISyntaxException, IOException {
		URI unzippedFileAccess = CheckSlicePoints.class.getResource("points.json").toURI();
		Files.copy(sourceFile, new File(new File(unzippedFileAccess).getParentFile(), sourceFile.getName()));
	}
	
	public static void savePoints(Map<String, FillFile> testPoints) throws IOException, URISyntaxException {
		URI unzippedFileAccess = CheckSlicePoints.class.getResource("points.json").toURI();
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		mapper.writeValue(new File(unzippedFileAccess), new ArrayList<FillFile>(testPoints.values()));
	}
	
	public static Map<String, FillFile> loadPoints() throws IOException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		List<FillFile> points = mapper.readValue(CheckSlicePoints.class.getResourceAsStream("points.json"), new TypeReference<List<FillFile>>(){});
		HashMap<String, FillFile> data = new HashMap<>();
		for (FillFile file : points) {
			data.put(file.getFileName(), file);
		}
		return data;
	}
	
	@Test
	public void testPoints() {
		
	}
}
