package org.area515.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class TemplateEngine {
    private static final Logger logger = LogManager.getLogger();
	private static StringTemplateLoader templateLoader = new StringTemplateLoader();
	private static Configuration config = null;
	
	
	public static final TemplateExceptionHandler INFO_IGNORE_HANDLER = new TemplateExceptionHandler() {
		public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
			logger.error("Logged error in template", te);
		}
	};

	public static String convertToFreeMarkerTemplate(String template) {
		if (template == null || template.trim().length() == 0) {
			return template;
		}
		String[] replacements = new String[] {
				"CURSLICE", 
				"LayerThickness", 
				"bulbHours", 
				"shutterOpen", 
				"ZDir", 
				"ZLiftRate", 
				"ZLiftDist", 
				"buildAreaMM", 
				"LayerTime", 
				"FirstLayerTime", 
				"NumFirstLayers",
				"buildPlatformXPixels",
				"buildPlatformYPixels"};
		for (String replacement : replacements) {
			template = template.replaceAll("\\$" + replacement, "\\$\\{" + replacement + "\\}");
		}
		
		return template;
	}
	
	public static String buildData(PrintJob job, Printer printer, String templateString) throws IOException, TemplateException {
		if (config == null) {
	        config = new Configuration(Configuration.VERSION_2_3_21);
	        config.setDefaultEncoding("UTF-8");
	        config.setTemplateExceptionHandler(INFO_IGNORE_HANDLER);
	        config.setTemplateLoader(templateLoader);
	        config.setBooleanFormat("yes,no");
		}
		
		//com.cfs.daq.script.SharedInterpreter has similar stuff in it...
        Map<String, Object> root = new HashMap<String, Object>();
        /*
        	$ZDir
        	$CURSLICE
        	$LayerThickness// the thickness of the layer in mm
        	$ZLiftDist// how far we're lifting
        	$ZLiftRate// the rate at which we're lifting
        $ZBottomLiftRate// the rate at which we're lifting for the bottom layers
        $ZRetractRate// how fast we'r retracting
        	$SlideTiltVal// any used slide / tilt value on the x axis
        $BlankTime// how long to show the blank in ms
        	$LayerTime// total delay for a layer for gcode commands to complete - not including expusre time
        	$FirstLayerTime// time to expose the first layers in ms
        	$NumFirstLayers// number of first layers
        */

		root.put("now", new Date());
		root.put("shutterOpen", printer.isShutterOpen());
		root.put("bulbHours", printer.getCachedBulbHours());
		root.put("CURSLICE", job.getCurrentSlice());
		root.put("LayerThickness", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getSliceHeight());
		root.put("ZDir", printer.getConfiguration().getSlicingProfile().getDirection().getVector());
		root.put("ZLiftRate", job.getZLiftSpeed());
		root.put("ZLiftDist", job.getZLiftDistance());
		Double buildArea = job.getPrintFileProcessor().getBuildAreaMM(job);
		root.put("buildAreaMM", buildArea == null || buildArea < 0?null:buildArea);
		root.put("LayerTime", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getExposureTime());
		root.put("FirstLayerTime", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getFirstLayerExposureTime());
		root.put("NumFirstLayers", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getNumberOfFirstLayers());
		root.put("SlideTiltVal", printer.getConfiguration().getSlicingProfile().getSlideTiltValue());
		root.put("buildPlatformXPixels", printer.getConfiguration().getSlicingProfile().getxResolution());
		root.put("buildPlatformYPixels", printer.getConfiguration().getSlicingProfile().getyResolution());
		root.put("job", job);
		root.put("printer", printer);
		
        /* Get the template (uses cache internally) */
        Object source = templateLoader.findTemplateSource(templateString);
        if (source == null) {
        	templateLoader.putTemplate(templateString, templateString);
        }
        Template template = config.getTemplate(templateString);
        template.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        try {
	        Writer out = new StringWriter();
	        template.process(root, out);
	        return out.toString();
        } catch (TemplateException e) {
        	
        	//TODO: this is a bit of a gray area, we aren't throwing an exception when they use buildAreaMM/bulbHours in something that doesn't use the print processor, but should we???
        	
        	//This means that buildAreaMM isn't supported for this printer
        	if (e.getBlamedExpressionString().equals("buildAreaMM") && e.getMessage().contains("The following has evaluated to null or missing")) {
        		logger.error("buildAreaMM was used in a template:" + templateString + ", but isn't supported by this print processor.");
        		return null;
        	}

        	//This means that bulbHours isn't supported for this printer
        	if (e.getBlamedExpressionString().equals("bulbHours") && e.getMessage().contains("The following has evaluated to null or missing")) {
        		logger.error("bulbHours was used in a template:" + templateString + ", but isn't supported by this projector model:" + printer.getProjectorModel());
        		return null;
        	}
        	
        	throw e;
        }
	}
	
	public static Object runScript(PrintJob job, Printer printer, ScriptEngine engine, String script, String scriptName, Map<String, Object> overrides) throws ScriptException {
		engine.put("now", new Date());
		engine.put("$shutterOpen", printer.isShutterOpen());
		Integer bulbHours = printer.getCachedBulbHours();
		engine.put("$bulbHours", bulbHours == null || bulbHours < 0?Double.NaN:new Double(bulbHours));
		engine.put("$CURSLICE", job.getCurrentSlice());
		engine.put("$LayerThickness", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getSliceHeight());
		engine.put("$ZDir", printer.getConfiguration().getSlicingProfile().getDirection().getVector());
		engine.put("$ZLiftRate", job.getZLiftSpeed());
		engine.put("$ZLiftDist", job.getZLiftDistance());
		Double buildArea = job.getPrintFileProcessor().getBuildAreaMM(job);
		engine.put("$buildAreaMM", buildArea == null || buildArea < 0?Double.NaN:buildArea);
		engine.put("$LayerTime", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getExposureTime());
		engine.put("$FirstLayerTime", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getFirstLayerExposureTime());
		engine.put("$NumFirstLayers", printer.getConfiguration().getSlicingProfile().getSelectedInkConfig().getNumberOfFirstLayers());
		engine.put("$SlideTiltVal", printer.getConfiguration().getSlicingProfile().getSlideTiltValue());
		engine.put("$buildPlatformXPixels", printer.getConfiguration().getSlicingProfile().getxResolution());
		engine.put("$buildPlatformYPixels", printer.getConfiguration().getSlicingProfile().getyResolution());
		engine.put("job", job);
		engine.put("printer", printer);
		engine.put(ScriptEngine.FILENAME, scriptName);
		
		if (overrides != null) {
			Iterator<Map.Entry<String, Object>> entries = overrides.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<String, Object> entry = entries.next();
				engine.put(entry.getKey(), entry.getValue());
			}
		}
		return engine.eval(script);
	}
}
