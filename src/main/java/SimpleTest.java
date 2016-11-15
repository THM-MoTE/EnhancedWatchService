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
import ews.PathFilter;
import ews.WatchServiceListener;


public class SimpleTest {

 public static void executionTest() throws IOException, InterruptedException {

     Path rootDir = Paths.get(System.getProperty("user.home")).resolve(
								       "Downloads");
     WatchEvent.Kind<?>[] events = new WatchEvent.Kind<?>[] {
	 StandardWatchEventKinds.ENTRY_CREATE,
	     StandardWatchEventKinds.ENTRY_DELETE,
	     StandardWatchEventKinds.ENTRY_MODIFY };

     ExecutorService pool = Executors.newFixedThreadPool(2);

     EnhancedWatchService service = new EnhancedWatchService(rootDir, true,
							     events);

     PathFilter filter = new PathFilter() {
      @Override
      public boolean acceptFile(Path file) {
        try {
          return !Files.isHidden(file)
              && !file.getFileName().toString().endsWith(".c");
        } catch (IOException e) {
          return false;
        }
      }

      @Override
      public boolean acceptDirectory(Path directory) {
        try {
          return !Files.isHidden(directory) && !directory.endsWith("Videos")
              && !directory.endsWith("target") && !directory.endsWith("bin");
        } catch (IOException e) {
          return false;
        }
      }
    };


    WatchServiceListener listener = new WatchServiceListener() {
      @Override
      public void modified(Path path) {
        System.out
        .println("file modified " + path);
      }

      @Override
      public void deleted(Path path) {
        System.out
        .println("file deleted " + path);
      }

      @Override
      public void created(Path path) {
        System.out
        .println("file created " + path);
      }
    };

     Runnable runnable = service.setup(listener, filter);
     Future<?> future = pool.submit(runnable);
     Thread.sleep(60_000);
     pool.shutdown();
     future.cancel(true);
     boolean flag = pool.awaitTermination(10, TimeUnit.SECONDS);
     System.out.println("shutdowned " + flag);

     if(flag) System.out.println("shutdown successfull");
     else System.err.println("shutdown failed");
 }

 public static void main(String args[]) throws IOException, InterruptedException {
   executionTest();
 }
}
