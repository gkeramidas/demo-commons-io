/*
 * MonitorDemo - a demo program for commons-io and the file monitor interface
 *
 * The 'monitor' interface of commons-io supports watching for changes in a set of files, and
 * publishes change events when files are modified, deleted or updated.  The commons-io abstraction
 * layer encodes the file monitoring API in a platform-independent manner, and falls back to polling
 * when the underlying OS does not support a smarter file system monitoring method.
 *
 * This demo program can accept a list of files or directories as command-line arguments, and then
 * it sets up a monitoring thread for changes in those files.  Every time a file is created, deleted
 * or modified under those paths it shows a message like:
 *
 *    C new/file/path/name
 *    M some/modified/file/path/name
 *    D deleted/file/name
 *
 * The design of the API for Apache Commons IO's version of the monitoring interface is pretty
 * generic, in the sense that it allows arbitrary control over:
 *
 *    - The number of file system monitoring threads spawned by the program.
 *    - The specific 'file observers' attached to each monitoring thread.
 *    - The listeners notified by each observer when a change event is posted.
 *
 * The API has three separate layers of objects, with file change events 'flowing' from one or more
 * `FileAlterationMonitor` threads to their associated `FileAlterationObserver` objects.  Then each
 * `FileAlterationObserver` may notify one or more `FileAlterationListenerAdaptor`s for each event.
 *
 * The extra flexibility of decoupled monitoring threads, multiple observers per monitoring thread,
 * and multiple listeners per observer is that one can treat each one of the three layers as a
 * separate layer of 'logic'.  For example, there's nothing that stops observers from being attached
 * to the same thread, and there's nothing that stops listeners from being notified by more than one
 * observer, as shown in the diagram below:
 *
 *                      +-----------------------+                   +-----------------------+
 *                      | FileAlterationMonitor |                   | FileAlterationMonitor |
 *                      +----------x------------+                   +-----------x-----------+
 *                                 |                                            |
 *                  .--------------'------------.                               |
 *                  |                           |                               |
 *      +-----------x------------+  +-----------x------------+      +-----------x------------+
 *      | FileAlterationObserver |  | FileAlterationObserver |      | FileAlterationObserver |
 *      +-----------x------------+  +------------x-----------+      +-----------x------------+
 *                  |                            |                              |
 *                  |                            '----------.         .---------'
 *                  |                                       |         |
 *  +---------------x---------------+            +----------x---------x----------+
 *  | FileAlterationListenerAdaptor |            | FileAlterationListenerAdaptor |
 *  +-------------------------------+            +-------------------------------+
 *
 * Note that each observer is associated with a single [java.io.File], but multiple observers can
 * register their interest by attaching to the same monitoring thread.  For example, the following
 * demo program uses a separate observer for each file name passed as a command-line argument.  All
 * these observers are attached to one monitoring thread, and all the observers post their file
 * change events to the same listener.  The layout of objects in this demo program is:
 *
 *                                   +-----------------------+
 *                                   | FileAlterationMonitor |
 *                                   +----------x------------+
 *                                              |
 *                  .---------------------------+---------------------------.
 *                  |                           |                           |
 *      +-----------x------------+  +-----------x------------+  +-----------x------------+
 *      | FileAlterationObserver |  | FileAlterationObserver |  | FileAlterationObserver |
 *      +-----------x------------+  +------------x-----------+  +-----------x------------+
 *                  |                            |                              |
 *                  '----------------------------+------------------------------'
 *                                               |
 *                               +---------------x---------------+
 *                               | FileAlterationListenerAdaptor |
 *                               +-------------------------------+
 *
 * A single `FileAlterationListenerAdaptor` receives all file system change events, and prints a
 * short descriptive line for each change event.
 */

package net.keramida.demo.commons.io

import java.io.File
import java.util.concurrent.TimeUnit

import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor

/**
 * Implements a demo program for the file monitoring interface of commons-io.  Each command-line
 * argument is used as the name of a file or directory to watch for changes.  Once a monitor has
 * been set up with observers for all the command-line arguments, a monitoring thread is spawned,
 * and the program lets it run forever.
 */
object MonitorDemo {
  def main(args: Array[String]): Unit = {
    /** The listener which actually reports what has been modified. Shared by all file observers. */
    val listener = new FileAlterationListenerAdaptor {
      override def onFileCreate(file: File) = println("C %s".format(file.getCanonicalPath))
      override def onFileDelete(file: File) = println("D %s".format(file.getCanonicalPath))
      override def onFileChange(file: File) = println("M %s".format(file.getCanonicalPath))
    }

    // Build a list of observers which can monitor separate [[java.io.File]] objects.
    // Each observer prints a message about the file it is watching.
    val files = args.map{f => new File(f)}
    val observers = files.map( fn =>
      new FileAlterationObserver(fn) {
        println("watching path: %s".format(fn.getCanonicalPath))
      }
    )

    // Attach the same listener to all observers.  When one of the observers reports a file system
    // change, the listener will report it, regardless of the observer which triggered it.  Each
    // listener callback function gets a [[java.io.File]] object, opened at the place of the file
    // system update.
    observers.map{ _.addListener(listener) }

    // A monitoring thread which polls for file system updates every 5 seconds.  If a change
    // <em>has</em> been made, it will trigger the appropriate observer(s) for the affected files.
    val monitor = new FileAlterationMonitor(TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS))

    // Attach all observers to the same polling monitor.
    observers.map{ o => monitor.addObserver(o) }

    monitor.start                                 // Start monitoring the file system for changes.
  }
}
