package wcs.core;

import COM.FutureTense.Interfaces.ICS;

/**
 * Interface for the Router
 * 
 * @author msciab
 * 
 */

public interface Router {

	public void route(ICS ics, String path, String query);

}
