package moe.plushie.armourers_workshop.core.skin;

import moe.plushie.armourers_workshop.core.data.DataDomain;
import moe.plushie.armourers_workshop.core.data.DataManager;
import moe.plushie.armourers_workshop.core.data.LocalDataService;
import moe.plushie.armourers_workshop.core.network.NetworkHandler;
import moe.plushie.armourers_workshop.core.network.packet.RequestSkinPacket;
import moe.plushie.armourers_workshop.core.utils.ResultHandler;
import moe.plushie.armourers_workshop.core.utils.SkinIOUtils;
import moe.plushie.armourers_workshop.init.common.AWCore;
import moe.plushie.armourers_workshop.init.common.ModContext;
import moe.plushie.armourers_workshop.init.common.ModLog;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class SkinLoader {

    private static final SkinLoader LOADER = new SkinLoader();

    private final EnumMap<DataDomain, Session> taskManager = new EnumMap<>(DataDomain.class);

    private final HashMap<String, Entry> entries = new HashMap<>();
    private final HashMap<String, ResultHandler<Skin>> waiting = new HashMap<>();

    private SkinLoader() {
        setup(null);
    }

    public static SkinLoader getInstance() {
        return LOADER;
    }

    public void setup(@Nullable MinecraftServer server) {
        LocalDataSession local = new LocalDataSession();
        if (server != null) {
            DownloadSession download = new DownloadSession();
            taskManager.put(DataDomain.LOCAL, local);
            taskManager.put(DataDomain.DATABASE, local);
            taskManager.put(DataDomain.DATABASE_LINK, local);
            taskManager.put(DataDomain.DEDICATED_SERVER, local);
            taskManager.put(DataDomain.GLOBAL_SERVER, download);
        } else {
            ProxySession proxy = new ProxySession();
            taskManager.put(DataDomain.LOCAL, local);
            taskManager.put(DataDomain.DATABASE, proxy);
            taskManager.put(DataDomain.DATABASE_LINK, proxy);
            taskManager.put(DataDomain.DEDICATED_SERVER, proxy);
            taskManager.put(DataDomain.GLOBAL_SERVER, proxy);
        }
    }

    @Nullable
    public Skin getSkin(ItemStack itemStack) {
        SkinDescriptor descriptor = SkinDescriptor.of(itemStack);
        if (descriptor.isEmpty()) {
            return null;
        }
        return getSkin(descriptor.getIdentifier());
    }

    @Nullable
    public Skin getSkin(String identifier) {
        if (identifier.isEmpty()) {
            return null;
        }
        Entry entry = getEntry(identifier);
        if (entry != null) {
            return entry.get();
        }
        return null;
    }

    @Nullable
    public Skin loadSkin(String identifier) {
        if (identifier.isEmpty()) {
            return null;
        }
        Entry entry = getOrCreateEntry(identifier);
        resumeRequest(entry, Method.SYNC);
        return entry.get();
    }

    public void loadSkin(String identifier, @Nullable ResultHandler<Skin> handler) {
        Entry entry = getOrCreateEntry(identifier);
        entry.notify(handler);
        resumeRequest(entry, Method.ASYNC);
    }

    public String saveSkin(String identifier, Skin skin) {
        if (DataDomain.isDatabase(identifier)) {
            return identifier;
        }
        String newIdentifier = LocalDataService.getInstance().addFile(skin);
        if (newIdentifier != null) {
            identifier = DataDomain.DATABASE.normalize(newIdentifier);
            addSkin(identifier, skin);
        }
        return identifier;
    }


    public void addSkin(String identifier, Skin skin) {
        Entry entry = getOrCreateEntry(identifier);
        entry.accept(skin);
    }

    public void addSkin(String identifier, Skin skin, Exception exception) {
        ModLog.debug("'{}' => receive server skin, exception: {}", identifier, exception);
        ResultHandler<Skin> resultHandler = waiting.remove(identifier);
        if (resultHandler != null) {
            resultHandler.apply(skin, exception);
        }
    }

    public void removeSkin(String identifier) {
        Entry entry = removeEntry(identifier);
        if (entry != null && !entry.status.isCompleted()) {
            entry.abort(new CancellationException("removed by user"));
        }
    }

    public synchronized void clear() {
        waiting.clear();
        entries.clear();
        setup(null);
    }

    private synchronized Entry getEntry(String identifier) {
        return entries.get(identifier);
    }

    private synchronized Entry getOrCreateEntry(String identifier) {
        return entries.computeIfAbsent(identifier, Entry::new);
    }

    private synchronized Entry removeEntry(String identifier) {
        return entries.remove(identifier);
    }

    private void resumeRequest(Entry entry, Method method) {
        if (entry.status.isCompleted()) {
            return;
        }
        Session session = taskManager.get(DataDomain.byName(entry.identifier));
        if (session == null) {
            entry.abort(new NoSuchElementException("can't found session"));
            return;
        }
        Request loading = session.request(method, entry.identifier);
        loading.delegate = entry;
    }

    public enum Status {
        PENDING, LOADING, FINISHED, CANCELLED, ABORTED;

        public boolean isCompleted() {
            return this == FINISHED || this == ABORTED;
        }
    }

    public enum Method {
        ASYNC, SOFT_SYNC, SYNC
    }

    public static class Entry {

        public final String identifier;

        public Skin skin;
        public Exception exception;
        public Status status = Status.PENDING;

        public ArrayList<ResultHandler<Skin>> handlers = new ArrayList<>();

        public Entry(String identifier) {
            this.identifier = identifier;
        }

        public void accept(Skin skin) {
            ModLog.debug("'{}' => finish skin loading", identifier);
            this.skin = skin;
            this.exception = null;
            this.status = Status.FINISHED;
            this.invoke();
        }

        public void abort(Exception exception) {
            ModLog.debug("'{}' => abort skin loading, exception: {}", identifier, exception);
            this.skin = null;
            this.exception = exception;
            this.status = Status.ABORTED;
            // when load is time out, we'll load it again the next time
            if (exception instanceof TimeoutException || exception instanceof CancellationException) {
                this.status = Status.PENDING;
            }
            this.invoke();
        }

        public void invoke() {
            ArrayList<ResultHandler<Skin>> handlers = this.handlers;
            this.handlers = new ArrayList<>();
            handlers.forEach(handler -> handler.apply(skin, exception));
        }

        public void notify(@Nullable ResultHandler<Skin> handler) {
            if (status.isCompleted()) {
                if (handler != null) {
                    handler.apply(skin, exception);
                }
                return;
            }
            if (handler != null) {
                handlers.add(handler);
            }
        }

        public Skin get() {
            return skin;
        }
    }

    public static class Request {

        public final String identifier;
        public int level = 0;
        public Skin skin;
        public Exception exception;
        public Method method = Method.ASYNC;
        public Entry delegate;

        public Request(String identifier) {
            this.identifier = identifier;
        }

        public void accept(Skin skin) {
            if (this.delegate != null) {
                this.delegate.accept(skin);
                this.delegate = null;
            }
        }

        public void abort(Exception exception) {
            if (this.delegate != null) {
                this.delegate.abort(exception);
                this.delegate = null;
            }
        }

        public void elevate(Method method) {
            this.level += 1;
            if (this.method.ordinal() < method.ordinal()) {
                this.method = method;
            }
        }
    }

    public static abstract class Session {

        protected final HashMap<String, Request> requests = new HashMap<>();

        protected final ExecutorService executor;

        private boolean isRunning = false;

        public Session(String name) {
            this.executor = buildThreadPool(name, 1);
        }

        public abstract Skin load(Request request) throws Exception;

        public Request request(Method method, String identifier) {
            Request task = getRequest(identifier);
            task.elevate(method);
            if (task.method != Method.ASYNC) {
                run(task);
            } else {
                start();
            }
            return task;
        }

        protected void run() {
            while (true) {
                Request task = pollRequest();
                if (task != null) {
                    run(task);
                } else {
                    break;
                }
            }
            synchronized (this) {
                isRunning = false;
            }
        }

        protected synchronized void start() {
            if (!isRunning) {
                isRunning = true;
                executor.execute(this::run);
            }
        }

        protected void run(Request request) {
            ModLog.debug("'{}' => start load skin", request.identifier);
            try {
                Skin skin = load(request);
                request.accept(skin);
            } catch (Exception exception) {
                request.abort(exception);
            }
        }

        protected ExecutorService buildThreadPool(String name, int size) {
            return Executors.newFixedThreadPool(size, r -> {
                Thread thread = new Thread(r, name);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });
        }

        protected synchronized Request pollRequest() {
            if (requests.isEmpty()) {
                return null;
            }
            Request request = Collections.max(requests.values(), Comparator.comparingInt(t -> t.level));
            if (request != null) {
                return requests.remove(request.identifier);
            }
            return null;
        }

        protected synchronized Request getRequest(String identifier) {
            return requests.computeIfAbsent(identifier, Request::new);
        }
    }

    public static abstract class LoadingSession extends Session {

        public LoadingSession(String name) {
            super(name);
        }

        @Override
        public Skin load(Request request) throws Exception {
            InputStream inputStream = null;
            try {
                inputStream = from(request);
                Skin skin = SkinIOUtils.loadSkinFromStream2(inputStream);
                loadDidFinish(request, skin);
                return skin;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        public abstract void loadDidFinish(Request request, Skin skin);

        public abstract InputStream from(Request request) throws Exception;
    }

    public static class LocalDataSession extends LoadingSession {

        public LocalDataSession() {
            super("AW-SKIN-LC");
        }

        @Override
        public void loadDidFinish(Request request, Skin skin) {
            ModLog.debug("'{}' => did load skin from local session", request.identifier);
        }

        @Override
        public InputStream from(Request request) throws Exception {
            return DataManager.getInstance().loadSkinData3(request.identifier);
        }
    }

    public static class ProxySession extends Session {

        private final Semaphore available = new Semaphore(0, true);

        private final CacheSession caching = new CacheSession();

        public ProxySession() {
            super("AW-SKIN-PR");
        }

        @Override
        public Skin load(Request request) throws Exception {
            try {
                return caching.load(request);
            } catch (Exception ignored) {
            }
            ModLog.debug("'{}' => start request server skin", request.identifier);
            RequestSkinPacket req = new RequestSkinPacket(request.identifier);
            NetworkHandler.getInstance().sendToServer(req);
            return await(request);
        }

        public Skin await(Request request) throws Exception {
            LockState state = new LockState(available);
            SkinLoader.getInstance().waiting.put(request.identifier, (skin, exception) -> receive(request, state, skin, exception));
            boolean ignored = available.tryAcquire(30, TimeUnit.SECONDS);
            state.timeout();
            if (state.skin != null) {
                caching.add(request.identifier, state.skin);
                return state.skin;
            }
            throw state.exception;
        }

        public void receive(Request request, LockState state, Skin skin, Exception exception) {
            state.skin = skin;
            state.exception = exception;
            // still waiting to response
            if (state.available != null) {
                state.release();
                return;
            }
            // we are late, but we can still save the data to the cache.
            if (skin != null) {
                caching.add(request.identifier, skin);
            }
        }

        static class LockState {
            Semaphore available;
            Skin skin;
            Exception exception;

            LockState(Semaphore semaphore) {
                this.available = semaphore;
            }

            synchronized void timeout() {
                this.available = null;
            }

            synchronized void release() {
                Semaphore available = this.available;
                this.available = null;
                if (available != null) {
                    available.release();
                }
            }
        }
    }

    public static class DownloadSession extends LoadingSession {

        private final CacheSession caching = new CacheSession();

        public DownloadSession() {
            super("AW-SKIN-DL");
        }

        @Override
        public Skin load(Request request) throws Exception {
            File cachedFile = caching.cachingFile(request.identifier);
            if (cachedFile.exists()) {
                try {
                    return caching.load(request);
                } catch (Exception ignored) {
                }
            }
            InputStream inputStream = null;
            try {
                FileUtils.forceMkdirParent(cachedFile);
                if (cachedFile.exists()) {
                    FileUtils.forceDelete(cachedFile);
                }
                // to prevent incorrect stream due to network fluctuation during loading.
                // we first write skin data into disk and then read it again.
                inputStream = from(request);
                GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(cachedFile));
                IOUtils.copy(inputStream, outputStream);
                IOUtils.closeQuietly(outputStream, inputStream);
                // now we can safely access skin data.
                inputStream = new GZIPInputStream(new FileInputStream(cachedFile));
                Skin skin = SkinIOUtils.loadSkinFromStream2(inputStream);
                loadDidFinish(request, skin);
                return skin;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        @Override
        public void loadDidFinish(Request request, Skin skin) {
            ModLog.debug("'{}' => did load skin from download session", request.identifier);
            caching.add(request.identifier, skin);
        }

        @Override
        public InputStream from(Request request) throws Exception {
            String serverId = DataDomain.getPath(request.identifier);
            String fileName = "";
            String api = String.format("http://plushie.moe/armourers_workshop/download-skin.php?skinid=%s&skinFileName=%s", serverId, fileName);
            return new URL(api).openStream();
        }

        @Override
        protected ExecutorService buildThreadPool(String name, int size) {
            return super.buildThreadPool(name, 2);
        }
    }

    public static class CacheSession extends LoadingSession {

        public CacheSession() {
            super("AW-SKIN-CH");
        }

        public void add(String identifier, Skin skin) {
            File cachedFile = cachingFile(identifier);
            if (skin == null || cachedFile == null) {
                return;
            }
            // global data no need decrypt/encrypt
            if (DataDomain.GLOBAL_SERVER.matches(identifier)) {
                ModLog.debug("'{}' => add global skin cache", identifier);
                executor.execute(() -> {
                    FileOutputStream fileOutputStream = null;
                    GZIPOutputStream gzipOutputStream = null;
                    try {
                        FileUtils.forceMkdirParent(cachedFile);
                        if (cachedFile.exists()) {
                            FileUtils.forceDelete(cachedFile);
                        }
                        fileOutputStream = new FileOutputStream(cachedFile);
                        gzipOutputStream = new GZIPOutputStream(fileOutputStream);
                        SkinIOUtils.saveSkinToStream(gzipOutputStream, skin);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    IOUtils.closeQuietly(gzipOutputStream, fileOutputStream);
                });
                return;
            }
            byte[] x0 = ModContext.x0();
            byte[] x1 = ModContext.x1();
            if (x0 == null || x1 == null) {
                return;
            }
            ModLog.debug("'{}' => add skin cache", identifier);
            executor.execute(() -> {
                FileOutputStream fileOutputStream = null;
                CipherOutputStream cipherOutputStream = null;
                GZIPOutputStream gzipOutputStream = null;
                try {
                    FileUtils.forceMkdirParent(cachedFile);
                    if (cachedFile.exists()) {
                        FileUtils.forceDelete(cachedFile);
                    }
                    fileOutputStream = new FileOutputStream(cachedFile);
                    if (x1.length != 0) {
                        fileOutputStream.write(x0);
                        SecretKeySpec key = new SecretKeySpec(x1, "AES");
                        Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
                        aes.init(Cipher.ENCRYPT_MODE, key);
                        cipherOutputStream = new CipherOutputStream(fileOutputStream, aes);
                        gzipOutputStream = new GZIPOutputStream(cipherOutputStream);
                        SkinIOUtils.saveSkinToStream(gzipOutputStream, skin);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                IOUtils.closeQuietly(gzipOutputStream, cipherOutputStream, fileOutputStream);
            });
        }

        public void remove(String identifier) {
            File cachedFile = cachingFile(identifier);
            if (cachedFile == null || !cachedFile.exists()) {
                return;
            }
            ModLog.debug("'{}' => remove skin cache", identifier);
            executor.execute(() -> {
                FileUtils.deleteQuietly(cachedFile);
            });
        }

        @Override
        public void loadDidFinish(Request request, Skin skin) {
            ModLog.debug("'{}' => did load skin from cache session", request.identifier);
        }

        @Override
        public InputStream from(Request request) throws Exception {
            File cacheFile = cachingFile(request.identifier);
            if (cacheFile == null) {
                throw new FileNotFoundException(request.identifier);
            }
            // global data no need decrypt/encrypt
            if (DataDomain.GLOBAL_SERVER.matches(request.identifier)) {
                return new GZIPInputStream(new FileInputStream(cacheFile));
            }
            byte[] x0 = ModContext.x0();
            byte[] x1 = ModContext.x1();
            if (x0 == null || x1 == null) {
                throw new IllegalStateException("illegal context state");
            }
            InputStream inputStream = new FileInputStream(cacheFile);
            byte[] target = new byte[x0.length];
            int targetSize = inputStream.read(target, 0, target.length);
            if (targetSize != x0.length || !Arrays.equals(x0, target)) {
                throw new IllegalStateException("illegal cache signature");
            }
            SecretKeySpec key = new SecretKeySpec(x1, "AES");
            Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aes.init(Cipher.DECRYPT_MODE, key);
            return new GZIPInputStream(new CipherInputStream(inputStream, aes));
        }

        public File cachingFile(String identifier) {
            File cacheFile = null;
            UUID t0 = ModContext.t0();
            String namespace = DataDomain.getNamespace(identifier);
            if (DataDomain.GLOBAL_SERVER.matches(identifier)) {
                String path = DataDomain.getPath(identifier);
                File rootPath = new File(AWCore.getSkinCacheDirectory(), "00000000-0000-0000-0000-000000000000");
                cacheFile = new File(new File(rootPath, namespace), path + ".dat");
            } else if (t0 != null) {
                String path = DataDomain.getPath(identifier);
                File rootPath = new File(AWCore.getSkinCacheDirectory(), t0.toString());
                cacheFile = new File(new File(rootPath, namespace), ModContext.md5(path) + ".dat");
            }
            return cacheFile;
        }
    }
}