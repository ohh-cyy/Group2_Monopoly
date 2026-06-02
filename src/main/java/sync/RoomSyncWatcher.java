package sync;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 监听房间文件夹变化；有玩家操作写入文件时才触发回调（带防抖），避免定时轮询刷 UI。
 */
public final class RoomSyncWatcher implements AutoCloseable {

    private static final long DEBOUNCE_MS = 300;

    private WatchService watchService;
    private Thread watchThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start(RoomFolder folder, Runnable onChanged) throws IOException {
        close();
        running.set(true);
        watchService = FileSystems.getDefault().newWatchService();

        Path root = folder.getRoot();
        Path commands = folder.commandsDir();
        Files.createDirectories(commands);

        registerDir(root);
        registerDir(commands);

        watchThread = new Thread(() -> watchLoop(onChanged), "room-sync-watch");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void registerDir(Path dir) throws IOException {
        dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
    }

    private void watchLoop(Runnable onChanged) {
        long lastTrigger = 0;
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (ClosedWatchServiceException e) {
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            boolean relevant = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                Object ctx = event.context();
                if (ctx instanceof Path fileName) {
                    String name = fileName.toString();
                    if (name.endsWith(".tmp")) {
                        continue;
                    }
                    if (name.equals("room.properties")
                            || name.equals("public.ser")
                            || name.startsWith("private_")
                            || name.endsWith(".cmd")) {
                        relevant = true;
                    }
                }
            }
            key.reset();

            if (!relevant) {
                continue;
            }

            long now = System.currentTimeMillis();
            if (now - lastTrigger < DEBOUNCE_MS) {
                continue;
            }
            lastTrigger = now;

            try {
                Thread.sleep(DEBOUNCE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (running.get()) {
                onChanged.run();
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
        }
    }
}
