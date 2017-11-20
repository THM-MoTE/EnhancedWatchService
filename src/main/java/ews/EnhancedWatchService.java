/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * Licensed under MIT License
 */

package ews;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An enhanced WatchService based on {@link java.nio.file.WatchService
 * WatchService}. This WatchService can handle whole directory-structures and
 * simplifies the creation-process.
 *
 * @author N. Justus
 * @version 0.1
 * @since 0.1
 */
public class EnhancedWatchService {
	private final Logger log = LoggerFactory
			.getLogger(EnhancedWatchService.class);
	private final boolean recursive;
	private final Path rootDir;
	private PathFilter filter;
	private WatchService watcher;
	private final WatchEvent.Kind<?>[] events;
	private final Map<WatchKey, Path> keysToPathes;
	private WatchServiceListener listener;

	/**
	 * Creates a new EnhancedWatchService for the given rootDir and the given
	 * events.
	 *
	 * @param rootDir
	 *            directory that should get observed
	 * @param recursive
	 *            true if you like to observe the whole directory-structure
	 *            below rootDir; false for only the rootDir
	 * @param events
	 *            which events should trigger this observer?
	 */
	public EnhancedWatchService(Path rootDir, boolean recursive,
			WatchEvent.Kind<?>... events) {
		this.recursive = recursive;
		this.rootDir = rootDir;
		this.events = events;
		keysToPathes = new HashMap<>();
	}


	/**
	 * Creates a WatchService which monitors the rootDir from the constructor.
	 * Calls the given listener if some event occurs and uses the given filter
	 * for filtering accepted paths.
	 * @param listener listener which is called if an event occurs
	 * @param filter - filter for monitored paths
	 * @return a runnable that a ThreadPool can execute
	 * @throws IOException
	 *             If it's not possible to create the watchers for the
   *             directories.
   *             <P>
   *             Note: This doesn't include Exceptions that happen in the
   *             running WatchService.
   *             </P>
	 */
	public Runnable setup(WatchServiceListener listener, PathFilter filter) throws IOException {
	  this.listener = listener;
	  this.filter = filter;
	  this.watcher = FileSystems.getDefault().newWatchService();
	  generateWatchers(rootDir);
    log.debug("Generated watchers for {}", keysToPathes.values());
    return () -> processEvents();
	}

	private void generateWatchers(Path dir) throws IOException {
		keysToPathes.put(dir.register(watcher, events), dir);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			Iterator<Path> iter = stream.iterator();
			while (iter.hasNext()) {
				Path path = iter.next();
				if (recursive && filter.acceptDirectory(path)
						&& Files.isDirectory(path)) {
					keysToPathes.put(path.register(watcher, events), path);
					generateWatchers(path);
				}
			}
		}
	}

	private void processEvents() {
		while (!Thread.currentThread().isInterrupted()) {
			WatchKey key;
			// wait for any event; stop if thread get's interrupted
			try {
				key = this.watcher.take();
			} catch (InterruptedException x) {
				break;
			}

			// handle each event
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind != StandardWatchEventKinds.OVERFLOW) {
					@SuppressWarnings("unchecked")
					WatchEvent<Path> evPath = (WatchEvent<Path>) event;
					// relative path from event to absolute path
					Path relativePath = evPath.context();
					Path absolutePath = keysToPathes.get(key).resolve(
							relativePath);
					log.debug("Received event {} for {}", kind, absolutePath);

					if (filter.acceptFile(absolutePath) || filter.acceptDirectory(absolutePath)) {
						listener.dispatchKind(absolutePath, kind);
					}

					// if a directory was created; add this directory to the
					// WatchService
					if (recursive
							&& kind == StandardWatchEventKinds.ENTRY_CREATE
							&& Files.isDirectory(absolutePath)) {
						try {
							log.debug("Create new watcher for directory {}",
									absolutePath);
							generateWatchers(absolutePath);
						} catch (IOException e) {
							log.warn(
									"Couldn't create watcher for directory {}",
									absolutePath);
						}
					}
				}
			}
			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keysToPathes.remove(key);

				// all directories are inaccessible
				if (keysToPathes.isEmpty()) {
					break;
				}
			}
		}

		log.debug("Gonna die");
	}
}
