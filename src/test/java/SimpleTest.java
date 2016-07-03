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

import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleTest {
 @Test
 public void executionTest() throws IOException,
			InterruptedException {

     Path rootDir = Paths.get(System.getProperty("user.home")).resolve(
								       "Downloads");
     WatchEvent.Kind<?>[] events = new WatchEvent.Kind<?>[] {
	 StandardWatchEventKinds.ENTRY_CREATE,
	     StandardWatchEventKinds.ENTRY_DELETE,
	     StandardWatchEventKinds.ENTRY_MODIFY };

     ExecutorService pool = Executors.newFixedThreadPool(2);

     BiConsumer<Path, Kind<?>> listener = (path, kind) -> System.out
	 .println("file changed " + path + " with mode " + kind);

     Predicate<Path> dirFilter = path -> {
	 try {
	     return !Files.isHidden(path) && !path.endsWith("Videos")
	     && !path.endsWith("target") && !path.endsWith("bin");
	 } catch (IOException e) {
	     return false;
	 }
     };

     Predicate<Path> fileFilter = path -> {
	 try {
	     return !Files.isHidden(path)
	     && !path.getFileName().toString().endsWith(".c");
	 } catch (IOException e) {
	     return false;
	 }
     };

     EnhancedWatchService service = new EnhancedWatchService(rootDir, true,
							     events);
     Future<?> future = service.start(pool, listener, dirFilter, fileFilter);
     Thread.sleep(10000);
     pool.shutdown();
     future.cancel(true);
     boolean flag = pool.awaitTermination(10, TimeUnit.SECONDS);
     System.out.println("shutdowned " + flag);
     assertTrue(flag);
 }
}
