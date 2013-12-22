package wcs.core;

// **** DANGER ZONE ****
// This file is critical and difficult to test
// change it at your own risk.
// **********************

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;

import wcs.api.Log;

/**
 * Load a pool of jars using a classloader. It caches the classloader and check
 * if the underlying jar file are changed before reloading it. A check is done
 * only after an interval passes.
 * 
 * @author msciab
 * 
 */
public class Loader {

	final static Log log = Log.getLog(Loader.class);

	private File jarDir;
	private long nextCheck = 0;
	private long jarTimeStamp;
	private int reloadInterval = 0;

	private ClassLoader parentClassLoader;
	private ClassLoader currentClassLoader;
	private File currentSpoolDir;

	public String getCurrentSpoolDir() {
		return currentSpoolDir.getAbsolutePath();
	}

	/**
	 * Build a loader
	 * 
	 * @param interval
	 *            for polling
	 * @param dir
	 *            where locate the jars
	 * @param cl
	 *            parent classloader
	 */
	public Loader(File dir, int interval, ClassLoader cl) {
		jarDir = dir;

		// reset spool dir to a known state
		cleanup();

		parentClassLoader = cl;
		currentClassLoader = parentClassLoader;
		currentSpoolDir = null;
		reloadInterval = interval;
		nextCheck = System.currentTimeMillis();
	}

	private void cleanup() {
		for (File sdir : jarDir.listFiles(onlyDirs)) {
			if (currentSpoolDir != null
					&& !currentSpoolDir.getAbsolutePath().equals(
							sdir.getAbsolutePath()))
				removeDir(sdir);
		}
	}

	/**
	 * Build a default loader
	 * 
	 * @param file
	 */
	public Loader() {
		jarDir = null;
		reloadInterval = 0;
	}

	/**
	 * Return the parent classloader
	 */
	public ClassLoader getParentClassLoader() {
		return parentClassLoader;
	}

	/*
	 * private boolean copy(File source, File dest) { FileOutputStream fout =
	 * null; FileInputStream fin = null; try { fout = new
	 * FileOutputStream(dest); fin = new FileInputStream(source); int c =
	 * fin.read(); while (c != -1) { fout.write(c); c = fin.read(); } return
	 * true; } catch (Exception ex) { ex.printStackTrace(); return false; }
	 * finally { try { fin.close(); } catch (IOException e) { } try {
	 * fout.close(); } catch (IOException e) { } } }
	 */

	private boolean copy(File src, File dst) {
		Path ps = src.toPath();
		Path pd = dst.toPath();

		try {
			if (dst.exists())
				Files.copy(ps, pd, StandardCopyOption.REPLACE_EXISTING);
			else
				Files.copy(ps, pd, StandardCopyOption.COPY_ATTRIBUTES);
			pd.toFile().setLastModified(System.currentTimeMillis());
			if (log.trace())
				log.trace("copied %s", pd);
			return true;
		} catch (IOException e) {
			log.error(e, "Loader.copyJar");
			return false;
		}
	}

	private File copyJarsToTempDir(File dir, File[] jars) throws IOException {
		File spoolDir = new File(dir, wcs.Api.tmp());
		if (!spoolDir.exists())
			spoolDir.mkdirs();

		// copy files in the spool dir
		for (File source : jars) {
			if (source.isDirectory())
				continue;
			if (!source.getName().toLowerCase().endsWith(".jar"))
				continue;

			File dest = new File(spoolDir, source.getName());
			if (copy(source, dest)) {
				if (log.trace())
					log.trace("spooling (update) %s", source.getName());
			} else
				log.error("cannot copy %s", dest);
		}
		return spoolDir;
	}

	private void removeDir(File dir) {
		if (dir == null || !dir.exists())
			return;
		for (File file : dir.listFiles()) {
			if (!file.delete()) {
				log.warn("cannot delete old file %s", file.getAbsolutePath());
				file.deleteOnExit(); // try to delete on exit anyway
			} else if (log.trace())
				log.trace("removing %s", file.getAbsolutePath());
		}
		if (!dir.delete()) {
			log.warn("cannot delete old file %s", dir.getAbsolutePath());
			dir.deleteOnExit(); // try to delete on exit anyway
		}
	}

	private URL[] toUrlArray(List<File> files) throws MalformedURLException {
		URL[] res = new URL[files.size()];
		int i = 0;
		for (File file : files)
			res[i++] = file.toURI().toURL();
		return res;
	}

	/**
	 * Return a class loader allowing to access new jars. It copies jars to a
	 * spool dir before loading them.
	 * 
	 * @return
	 */
	public ClassLoader getClassLoader() {

		// get jars if somehing changed
		File[] jars = getJarsIfSomeIsModifiedAfterInterval();
		if (jars == null)
			return currentClassLoader;

		// update classloader
		synchronized (this) {
			try {
				// copy jars to a new spooldir
				File newSpoolDir = copyJarsToTempDir(jarDir, jars);
				// create a new classloader
				List<File> list = new LinkedList<File>();
				for (File f : newSpoolDir.listFiles(onlyJars))
					list.add(f);
				if (log.debug()) {
					System.out.println("Loader: reloader " + newSpoolDir);
					log.debug("reloaded from %s", newSpoolDir.toString());
					if (log.trace()) {
						for (File f : list)
							log.trace("jar %s", f.toString());
					}
				}

				// switch the class loader
				ClassLoader oldClassLoader = currentClassLoader;
				currentClassLoader = new URLClassLoader(toUrlArray(list),
						parentClassLoader);
				if (oldClassLoader != parentClassLoader
						&& oldClassLoader instanceof URLClassLoader)
					((URLClassLoader) oldClassLoader).close();

				currentSpoolDir = newSpoolDir;
				cleanup();

			} catch (Exception ex) {
				log.error(ex, "[Loader.getClassLoader]");
			}
			return currentClassLoader;
		}
	}

	/**
	 * Close the current classloader, freeing the underlying opened jars.
	 */
	public void close() {

		// close current classloader
		if (currentClassLoader instanceof URLClassLoader) {
			((JarClassLoader) currentClassLoader).close();
			currentClassLoader = parentClassLoader;
		}

		if (currentClassLoader instanceof URLClassLoader) {
			try {
				((URLClassLoader) currentClassLoader).close();
			} catch (IOException e) {
				log.error(e, "Loader.close");
			}
			currentClassLoader = parentClassLoader;
		}

	}

	private FileFilter onlyJars = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return !f.isDirectory()
					&& f.getName().toLowerCase().endsWith(".jar");
		}
	};

	private FileFilter onlyDirs = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return f.isDirectory();
		}
	};

	/**
	 * Return the jars to use only for the classloader if some of them has been
	 * modified Check only once in a given interval.
	 * 
	 * @param jar
	 * @return
	 * @throws MalformedURLException
	 */
	public File[] getJarsIfSomeIsModifiedAfterInterval() {

		// ********************
		// DO NOT CHANGE THIS METHOD WITHOUT CAREFUL TESTING
		// IT IS ACCESSED HEAVILY CONCURRENTLY
		// ********************

		// check it is is time to check
		// do it in an unsychronized way
		long now = System.currentTimeMillis();
		if (now < nextCheck)
			return null;

		synchronized (this) {

			// some time was spent trying to acquire the lock
			// someone else may have updated the time to check
			// checkiung again what is the time
			// if someone updated the nextCheckTime
			// update time for the next check
			now = System.currentTimeMillis();
			if (now < nextCheck)
				return null;

			// set next check time
			nextCheck = nextCheck + reloadInterval;

			// get the more recent lastmodified timestamp
			long curTimeStamp = 0;

			File[] jars = jarDir.listFiles(onlyJars);
			if (jars.length == 0) {
				log.warn("no jars in the jar folder");
				return null;
			}

			for (File file : jars) {
				curTimeStamp = Math.max(curTimeStamp, file.lastModified());
			}

			// log.trace("curTimestamp=%d jarTimestamp=%d", curTimestamp,
			// jarTimestamp);
			if (curTimeStamp > jarTimeStamp) {
				if (log.debug()) {
					log.debug("jar changed, timestamp=%d", curTimeStamp);
					System.out.println("Loader: detected change");
				}
				jarTimeStamp = curTimeStamp;
				return jars;
			} else {
				if (log.trace())
					log.trace("no changes detected");
			}
			return null;
		}
	}

	/**
	 * Load a class for name from current class loaders
	 * 
	 * @param classname
	 * @return
	 */
	public Class<?> loadClass(String classname) {
		try {
			ClassLoader cl = getClassLoader();
			if (log.trace())
				log.trace("loading %s", classname);
			return Class.forName(classname, true, cl);
		} catch (ClassNotFoundException ex) {
			log.error(ex, "[Loader.loadClass]");
			return null;
		}
	}
}
