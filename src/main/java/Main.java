/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * Licensed under MIT License
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import ews.EnhancedWatchService;


public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		Path rootDir = Paths.get(System.getProperty("user.home"));
		WatchEvent.Kind<?>[] events = new WatchEvent.Kind<?>[] {
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY
		};
		
		ExecutorService pool = Executors.newFixedThreadPool(2);
		
		BiConsumer<Path, Kind<?>> listener = (path, kind) -> System.out.println("file changed "+path+" with mode "+kind);
		
		Predicate<Path> dirFilter = path -> {
			try {
				return !Files.isHidden(path) &&
						!path.endsWith("Videos") &&
						!path.endsWith("target") &&
						!path.endsWith("bin");
			} catch(IOException e) { return false; }
		};
		
		Predicate<Path> fileFilter = path -> {
			try {
				return !Files.isHidden(path) && !path.getFileName().toString().endsWith(".c");
			} catch(IOException e) { return false; }
		};
		
		EnhancedWatchService service = new EnhancedWatchService(rootDir, true, events);
		Future<?> future = service.start(pool, listener, dirFilter, fileFilter);
		Thread.sleep(1000);
		boolean flag = pool.awaitTermination(10, TimeUnit.MINUTES);
		pool.shutdown();
		future.cancel(true);
		System.out.println("shutdowned "+flag);
	}
}
