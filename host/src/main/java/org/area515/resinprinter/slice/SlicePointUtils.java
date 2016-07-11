package org.area515.resinprinter.slice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.Files;

public class SlicePointUtils {
	public static void copyFileToPackage(File sourceFile) throws URISyntaxException, IOException {
		URI unzippedFileAccess = SlicePointUtils.class.getResource("points.json").toURI();
		Files.copy(sourceFile, new File(new File(unzippedFileAccess).getParentFile(), sourceFile.getName()));
	}
	
	public static void savePoints(Map<FillFile, FillFile> testPoints) throws IOException, URISyntaxException {
		URI unzippedFileAccess = SlicePointUtils.class.getResource("points.json").toURI();
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(new File(unzippedFileAccess), new ArrayList<FillFile>(testPoints.values()));
		System.out.println("Saved to:" + new File(unzippedFileAccess).getAbsolutePath());
	}
	
	public static Map<FillFile, FillFile> loadPoints() throws IOException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		InputStream stream = SlicePointUtils.class.getResourceAsStream("points.json");
		List<FillFile> points = mapper.readValue(stream, new TypeReference<List<FillFile>>(){});
		
		HashMap<FillFile, FillFile> data = new HashMap<>();
		for (FillFile file : points) {
			data.put(file, file);
		}
		return data;
	}
	
	@Test
	public void testPoints() {
		
	}
}
