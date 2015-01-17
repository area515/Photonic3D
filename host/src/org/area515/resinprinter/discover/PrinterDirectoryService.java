package org.area515.resinprinter.discover;

import java.util.List;
import java.util.concurrent.Future;

import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.PlaylistContainer;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.TextItem;

public class PrinterDirectoryService extends AbstractContentDirectoryService {
	private TextItem ROOT = null;
	private final PrinterHostSettingsServiceManager<PrinterDirectoryService> manager;
	
	public PrinterDirectoryService(LocalService<PrinterDirectoryService> service) throws InterruptedException {
		this.ROOT = null;
		this.manager = (PrinterHostSettingsServiceManager)service.getManager();
	}
	
	@Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag,
            String filter, long firstResult, long maxResults,
            SortCriterion[] orderby) throws ContentDirectoryException {
		
    	if (ROOT == null) {
        	ROOT = new TextItem();
        	ROOT.setParentID("-1");
        	ROOT.setTitle("URI:" + manager.getUri());
        	ROOT.setCreator(manager.getSetup().getManufacturer());
        	ROOT.setId("0");
    	}
    	
        try {
    		BrowseResult browseResult;
            DIDLContent didl = new DIDLContent();
            didl.addItem(ROOT);
            browseResult = new BrowseResult(new DIDLParser().generate(didl), 1, 1);
            return browseResult;
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
        }
	}
}
