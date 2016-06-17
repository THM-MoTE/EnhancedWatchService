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

public class EnhancedWatchService {
	private final Logger log = LoggerFactory.getLogger(EnhancedWatchService.class);
	private final boolean recursive;
	private final Path rootDir;
	private Predicate<Path> fileFilter;
	private Predicate<Path> dirFilter;
	private WatchService watcher;
	private final WatchEvent.Kind<?>[] events;
	private final Map<WatchKey, Path> keysToPathes;
	private BiConsumer<Path, Kind<?>> callback;

	public EnhancedWatchService(Path rootDir, boolean recursive, WatchEvent.Kind<?>... events) {
		this.recursive = recursive;
		this.rootDir = rootDir;
		this.events = events;
		keysToPathes = new HashMap<>();
	}
	
	public Future<?> start(ExecutorService pool, BiConsumer<Path, Kind<?>> callback, Predicate<Path> dirFilter, Predicate<Path> fileFilter) throws IOException {
		this.fileFilter = fileFilter;
		this.dirFilter = dirFilter;
		this.watcher =  FileSystems.getDefault().newWatchService();
		this.callback = callback;
		generateWatchers(rootDir);
		log.debug("Generated watchers for {}", keysToPathes.values());
		return pool.submit(() -> processEvents());
	}
	
	private void generateWatchers(Path dir) throws IOException {
		keysToPathes.put(dir.register(watcher, events), dir);
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			Iterator<Path> iter = stream.iterator();
			while(iter.hasNext()) {
				Path path = iter.next();
				if(recursive &&
					dirFilter.test(path) &&
					Files.isDirectory(path)) {
					keysToPathes.put(path.register(watcher, events), path);
					generateWatchers(path);
				}
			}
		}
	}
	
	private void processEvents() {
		while(!Thread.currentThread().isInterrupted()) {
			WatchKey key;
			try {
				 key = this.watcher.take();
			} catch(InterruptedException x) {
		        break;
		    }
			
			for(WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if(kind != StandardWatchEventKinds.OVERFLOW) {
					@SuppressWarnings("unchecked")
					WatchEvent<Path> evPath = (WatchEvent<Path>)event;
			        Path relativePath = evPath.context();
			        Path absolutePath = keysToPathes.get(key).resolve(relativePath);
			        log.debug("Received event {} for {}", kind, absolutePath);
			        
			        if(fileFilter.test(absolutePath)) {
			        	callback.accept(absolutePath, kind);
			        }
			        
			        if(recursive &&
		        		kind == StandardWatchEventKinds.ENTRY_CREATE && 
		        		Files.isDirectory(absolutePath)) {
			        	try {
			        		log.debug("Create new watcher for directory {}", absolutePath);
							generateWatchers(absolutePath);
						} catch (IOException e) {
							log.warn("Couldn't create watcher for directory {}", absolutePath);
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
