/**
 * Back 2 Browser Bytecode Translator
 * Copyright (C) 2012-2018 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://opensource.org/licenses/GPL-2.0.
 */
package org.apidesign.bck2brwsr.launcher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apidesign.bck2brwsr.launcher.InvocationContext.Resource;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

/**
 * Lightweight server to launch Bck2Brwsr applications and tests.
 * Supports execution in native browser as well as Java's internal
 * execution engine.
 */
abstract class BaseHTTPLauncher extends Launcher implements Flushable, Closeable, Callable<HttpServer> {
    static final Logger LOG = Logger.getLogger(BaseHTTPLauncher.class.getName());
    private static final Logger OUT = Logger.getLogger(BaseHTTPLauncher.class.getName() + ".out");
    private static final InvocationContext END = new InvocationContext(null, null, null);
    private final Set<ClassLoader> loaders = new LinkedHashSet<>();
    private final BlockingQueue<InvocationContext> methods = new LinkedBlockingQueue<>();
    private long timeOut;
    private final Res resources = new Res();
    private final String cmd;
    private Object[] brwsr;
    private HttpServer server;
    private CountDownLatch wait;
    private Thread flushing;
    private String rootPage;
    private Integer exitCode;

    public BaseHTTPLauncher(String cmd) {
        this.cmd = cmd;
        addClassLoader(BaseHTTPLauncher.class.getClassLoader());
        setTimeout(180000);
    }

    @Override
    InvocationContext runMethod(InvocationContext c) throws IOException {
        loaders.add(c.clazz.getClassLoader());
        methods.add(c);
        try {
            c.await(timeOut);
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
        return c;
    }

    public final void setTimeout(long ms) {
        timeOut = ms;
    }

    @Override
    public final void addClassLoader(ClassLoader url) {
        this.loaders.add(url);
    }

    ClassLoader[] loaders() {
        return loaders.toArray(new ClassLoader[loaders.size()]);
    }

    @Override
    void rootPage(String startpage) {
        this.rootPage = startpage;
    }

    @Override
    public void showURL(String startpage) throws IOException {
        if (startpage.startsWith("http:") || startpage.startsWith("https:")) {
            try {
                URI fullUri = new URI(startpage);
                showBrwsr(fullUri);
                return;
            } catch (URISyntaxException ex) {
                // OK, go on
            }
        }

        if (!startpage.startsWith("/")) {
            startpage = "/" + startpage;
        }
        HttpServer s = initServer(".", true, "", 5000, false);
        int last = startpage.lastIndexOf('/');
        String prefix = startpage.substring(0, last);
        String simpleName = startpage.substring(last);
        s.getServerConfiguration().addHttpHandler(new SubTree(resources, prefix), "/");
        server = s;
        try {
            launchServerAndBrwsr(s, simpleName);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    void showDirectory(File dir, String startpage, boolean addClasses) throws IOException {
        if (!startpage.startsWith("/")) {
            startpage = "/" + startpage;
        }
        String prefix = null;
        if (!new File(dir, "bck2brwsr.js").exists()) {
            int last = startpage.lastIndexOf('/');
            if (last >= 0) {
                prefix = startpage.substring(0, last);
            }
        }

        Properties props = new Properties();
        File portFile = new File(dir, ".bck2brwsrPort");
        Integer port = null;
        if (portFile.exists()) {
            try (FileInputStream fis = new FileInputStream(portFile)) {
                props.load(fis);
            }
            String p = props.getProperty("port");
            if (p != null) {
                try {
                    port = Integer.parseInt(p);
                } catch (NumberFormatException ex) {
                    // go on
                }
            }
        }
        if (port != null) {
            URL url = new URL("http://localhost:" + port + "/heartbeat/reload");
            LOG.log(Level.INFO, "Requesting reload via {0}", url);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()))) {
                final String reply = r.readLine();
                if ("Reloaded".equals(reply)) {
                    synchronized (this) {
                        flushing = Thread.currentThread();
                        notifyAll();
                    }
                    LOG.log(Level.INFO, "Reload OK: {0}", reply);
                    return;
                }
                LOG.log(Level.INFO, "Reload rejected {0}", reply);
            } catch (IOException ex) {
                LOG.log(Level.INFO, "Reload rejected: " + ex.getMessage(), ex);
            }
        }

        HttpServer s = initServer(dir.getPath(), addClasses, prefix, 5000, false);
        try {
            launchServerAndBrwsr(s, startpage);
            props.setProperty("port", "" + findPort(s));
            try (FileOutputStream out = new FileOutputStream(portFile)) {
                props.store(out, cmd);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Cannot store port in {0}", portFile);
            }
            portFile.deleteOnExit();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void initialize() throws IOException {
        try {
            executeInBrowser();
        } catch (InterruptedException ex) {
            final InterruptedIOException iio = new InterruptedIOException(ex.getMessage());
            iio.initCause(ex);
            throw iio;
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException)ex;
            }
            if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            }
            throw new IOException(ex);
        }
    }

    private HttpServer initServer(
        String path, boolean addClasses, String vmPrefix, int countDownTime, boolean unitTests
    ) throws IOException {
        HttpServer s = HttpServer.createSimpleServer(null, new PortRange(8080, 65535));
        /*
        ThreadPoolConfig fewThreads = ThreadPoolConfig.defaultConfig().copy().
            setPoolName("Fx/Bck2 Brwsr").
            setCorePoolSize(3).
            setMaxPoolSize(5);
        ThreadPoolConfig oneKernel = ThreadPoolConfig.defaultConfig().copy().
            setPoolName("Kernel Fx/Bck2").
            setCorePoolSize(3).
            setMaxPoolSize(3);
        for (NetworkListener nl : s.getListeners()) {
            nl.getTransport().setWorkerThreadPoolConfig(fewThreads);
            nl.getTransport().setKernelThreadPoolConfig(oneKernel);
        }
*/
        final ServerConfiguration conf = s.getServerConfiguration();
        VMAndPages vm = new VMAndPages(unitTests);
        conf.addHttpHandler(vm, "/");
        if (vmPrefix != null) {
            vm.registerVM(vmPrefix + "/bck2brwsr.js");
        }
        if (path != null) {
            vm.addDocRoot(path);
        }
        conf.addHttpHandler(new Console(), "/console/");
        if (addClasses) {
            conf.addHttpHandler(new Classes(resources), "/classes/");
        }
        if (rootPage != null) {
            int last = rootPage.lastIndexOf('/');
            String prefix = rootPage.substring(0, last);
            String page = rootPage.substring(last);
            s.getServerConfiguration().addHttpHandler(new SubTree("/pages" + page, resources, prefix), "/pages/");
        }
        final WebSocketAddOn addon = new WebSocketAddOn();
        for (NetworkListener listener : s.getListeners()) {
            listener.registerAddOn(addon);
        }
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new java.util.logging.Formatter() {
            @Override
            public String format(LogRecord record) {
                String message = formatMessage(record);
                StringWriter w = new StringWriter();
                w.append(String.format("[%s] %s\n", record.getLevel(), message));
                if (record.getThrown() != null) {
                    record.getThrown().printStackTrace(new PrintWriter(w));
                }
                return w.toString();
            }
        });
        consoleHandler.setLevel(Level.ALL);

        Logger handleLogger = Logger.getLogger("org.glassfish.grizzly.http.server.HttpHandler");
        handleLogger.setLevel(Level.FINE);

        Logger serverLogger = Logger.getLogger("org.glassfish.grizzly.http.server");
        serverLogger.setLevel(Level.INFO);
        serverLogger.setUseParentHandlers(false);
        serverLogger.addHandler(consoleHandler);

        if (countDownTime > 0) {
            final LifeCycleApp lifeCycleApp = new LifeCycleApp(this, countDownTime, serverLogger);
            WebSocketEngine.getEngine().register("", "/heartbeat", lifeCycleApp);
            conf.addHttpHandler(lifeCycleApp.getReloadHandler(), "/heartbeat/reload");
        }

        LOG.addHandler(consoleHandler);
        LOG.setUseParentHandlers(false);
        return s;
    }

    private static int resourcesCount;
    private void executeInBrowser() throws InterruptedException, URISyntaxException, IOException {
        wait = new CountDownLatch(1);
        server = initServer(".", true, "", -1, true);
        final ServerConfiguration conf = server.getServerConfiguration();

        class DynamicResourceHandler extends HttpHandler {
            private final InvocationContext ic;
            DynamicResourceHandler delegate;
            public DynamicResourceHandler(InvocationContext ic) {
                this.ic = ic;
                for (Resource r : ic.resources) {
                    conf.addHttpHandler(this, r.httpPath);
                }
            }

            public void close(DynamicResourceHandler del) {
                conf.removeHttpHandler(this);
                delegate = del;
            }

            @Override
            public void service(Request request, Response response) throws Exception {
                if (delegate != null) {
                    delegate.service(request, response);
                    return;
                }

                if ("/dynamic".equals(request.getRequestURI())) {
                    boolean webSocket = false;
                    String mimeType = request.getParameter("mimeType");
                    List<String> params = new ArrayList<>();
                    for (int i = 0;; i++) {
                        String p = request.getParameter("param" + i);
                        if (p == null) {
                            break;
                        }
                        params.add(p);
                        if ("protocol:ws".equals(p)) {
                            webSocket = true;
                        }
                    }
                    final String cnt = request.getParameter("content");
                    String urlEncode = cnt.replace("%20", " ").replace("%0A", "\n");
                    ByteArrayInputStream is = new ByteArrayInputStream(urlEncode.getBytes("UTF-8"));
                    URI url;
                    final Resource res = new Resource(is, mimeType, "/dynamic/res" + ++resourcesCount, params.toArray(new String[params.size()]));
                    if (webSocket) {
                        url = registerWebSocket(res);
                    } else {
                        url = registerResource(res);
                    }
                    response.setHeader(Header.CacheControl, "no-cache");
                    response.setHeader(Header.Pragma, "no-cache");
                    response.getWriter().write(url.toString());
                    response.getWriter().write("\n");
                    return;
                }

                for (Resource r : ic.resources) {
                    if (r.httpPath.equals(request.getRequestURI())) {
                        LOG.log(Level.INFO, "Serving HttpResource for {0}", request.getRequestURI());
                        response.setContentType(r.httpType);
                        response.setHeader(Header.CacheControl, "no-cache");
                        response.setHeader(Header.Pragma, "no-cache");
                        r.httpContent.reset();
                        String[] params = null;
                        if (r.parameters.length != 0) {
                            params = new String[r.parameters.length];
                            for (int i = 0; i < r.parameters.length; i++) {
                                params[i] = request.getParameter(r.parameters[i]);
                                if (params[i] == null) {
                                    if ("http.method".equals(r.parameters[i])) {
                                        params[i] = request.getMethod().toString();
                                    } else if ("http.requestBody".equals(r.parameters[i])) {
                                        Reader rdr = request.getReader();
                                        StringBuilder sb = new StringBuilder();
                                        for (;;) {
                                            int ch = rdr.read();
                                            if (ch == -1) {
                                                break;
                                            }
                                            sb.append((char)ch);
                                        }
                                        params[i] = sb.toString();
                                    } else if (r.parameters[i].startsWith("http.header.")) {
                                        params[i] = request.getHeader(r.parameters[i].substring(12));
                                    }
                                }
                                if (params[i] == null) {
                                    params[i] = "null";
                                }
                            }
                        }

                        copyStream(r.httpContent, response.getOutputStream(), null, params);
                    }
                }
            }

            private URI registerWebSocket(Resource r) {
                WebSocketEngine.getEngine().register("", r.httpPath, new WS(r));
                return pageURL("ws", server, r.httpPath);
            }

            private URI registerResource(Resource r) {
                if (!ic.resources.contains(r)) {
                    ic.resources.add(r);
                    conf.addHttpHandler(this, r.httpPath);
                }
                return pageURL("http", server, r.httpPath);
            }
        }

        conf.addHttpHandler(new Page(resources, harnessResource()), "/execute");

        conf.addHttpHandler(new HttpHandler() {
            int cnt;
            List<InvocationContext> cases = new ArrayList<>();
            DynamicResourceHandler prev;
            @Override
            public void service(Request request, Response response) throws Exception {
                String id = request.getParameter("request");
                String value = request.getParameter("result");
                String timeText = request.getParameter("time");
                if (value != null && value.indexOf((char)0xC5) != -1) {
                    value = toUTF8(value);
                }
                int time;
                if (timeText != null) {
                    try {
                        time = (int) Double.parseDouble(timeText);
                    } catch (NumberFormatException numberFormatException) {
                        time = 0;
                    }
                } else {
                    time = 0;
                }

                InvocationContext mi = null;
                int caseNmbr = -1;

                if (id != null && value != null) {
                    LOG.log(Level.INFO, "Received result for case {0} = {1}", new Object[]{id, value});
                    value = decodeURL(value);
                    int indx = Integer.parseInt(id);
                    cases.get(indx).result(value, time, null);
                    if (++indx < cases.size()) {
                        mi = cases.get(indx);
                        LOG.log(Level.INFO, "Re-executing case {0}", indx);
                        caseNmbr = indx;
                    }
                } else {
                    if (!cases.isEmpty()) {
                        LOG.info("Re-executing test cases");
                        mi = cases.get(0);
                        caseNmbr = 0;
                    }
                }

                if (mi == null) {
                    mi = methods.take();
                    caseNmbr = cnt++;
                }
                final Writer w = response.getWriter();
                if (mi == END) {
                    w.write("");
                    wait.countDown();
                    cnt = 0;
                    LOG.log(Level.INFO, "End of data reached. Exiting.");
                    return;
                }
                final DynamicResourceHandler newRH = new DynamicResourceHandler(mi);
                if (prev != null) {
                    prev.close(newRH);
                }
                prev = newRH;
                conf.addHttpHandler(prev, "/dynamic");

                cases.add(mi);
                final String cn = mi.clazz.getName();
                final String mn = mi.methodName;
                LOG.log(Level.INFO, "Request for {0} case. Sending {1}.{2}", new Object[]{caseNmbr, cn, mn});
                w.write("{"
                    + "className: '" + cn + "', "
                    + "methodName: '" + mn + "', "
                    + "request: " + caseNmbr
                );
                if (mi.args != null) {
                    w.write(", args: [");
                    String sep = "";
                    for (String a : mi.args) {
                        w.write(sep);
                        w.write("'");
                        w.write(a);
                        w.write("'");
                        sep = ", ";
                    }
                    w.write("]");
                }
                if (mi.html != null) {
                    w.write(", html: '");
                    w.write(encodeJSON(mi.html));
                    w.write("'");
                }
                w.write("}");
            }
        }, "/data");

        String page = "/execute";
        if (rootPage != null) {
            int last = rootPage.lastIndexOf('/');
            page = "/pages" + rootPage.substring(last);
        }

        this.brwsr = launchServerAndBrwsr(server, page);
    }

    private static String encodeJSON(String in) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            if (ch < 32 || ch == '\'' || ch == '"') {
                sb.append("\\u");
                String hs = "0000" + Integer.toHexString(ch);
                hs = hs.substring(hs.length() - 4);
                sb.append(hs);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    @Override
    public void shutdown() throws IOException {
        shutdown(0, true);
    }

    private void shutdown(int exitCode, boolean wait4methods) throws IOException {
        synchronized (this) {
            if (this.exitCode != null) {
                return;
            }
            this.exitCode = exitCode;
            if (flushing != null) {
                flushing.interrupt();
                flushing = null;
            }
        }
        if (wait4methods) {
            methods.offer(END);
            for (;;) {
                int prev = methods.size();
                try {
                    if (wait != null && wait.await(timeOut, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException ex) {
                    throw new IOException(ex);
                }
                if (prev == methods.size()) {
                    LOG.log(
                        Level.WARNING,
                        "Timeout and no test has been executed meanwhile (at {0}). Giving up.",
                        methods.size()
                    );
                    break;
                }
                LOG.log(Level.INFO,
                    "Timeout, but tests got from {0} to {1}. Trying again.",
                    new Object[]{prev, methods.size()}
                );
            }
        }
        stopServerAndBrwsr(server, brwsr);
    }

    static void copyStream(InputStream is, OutputStream os, String baseURL, String... params) throws IOException {
        for (;;) {
            int ch = is.read();
            if (ch == -1) {
                break;
            }
            if (ch == '$' && params.length > 0) {
                int cnt = is.read() - '0';
                if (baseURL != null && cnt == 'U' - '0') {
                    os.write(baseURL.getBytes("UTF-8"));
                } else {
                    if (cnt >= 0 && cnt < params.length) {
                        os.write(params[cnt].getBytes("UTF-8"));
                    } else {
                        os.write('$');
                        os.write(cnt + '0');
                    }
                }
            } else {
                os.write(ch);
            }
        }
    }

    private Object[] launchServerAndBrwsr(HttpServer server, final String page) throws IOException, URISyntaxException, InterruptedException {
        server.start();
        URI uri = pageURL("http", server, page);
        return showBrwsr(uri);
    }
    private static String toUTF8(String value) throws UnsupportedEncodingException {
        byte[] arr = new byte[value.length()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte)value.charAt(i);
        }
        return new String(arr, "UTF-8");
    }

    private static String decodeURL(String s) {
        for (;;) {
            int pos = s.indexOf('%');
            if (pos == -1) {
                return s;
            }
            int i = Integer.parseInt(s.substring(pos + 1, pos + 2), 16);
            s = s.substring(0, pos) + (char)i + s.substring(pos + 2);
        }
    }

    private void stopServerAndBrwsr(HttpServer server, Object[] brwsr) throws IOException {
        if (brwsr == null) {
            return;
        }
        Process process = (Process)brwsr[0];

        server.stop();
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream();
        drain("StdOut", stdout);
        drain("StdErr", stderr);
        process.destroy();
        int res;
        try {
            res = process.waitFor();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
        LOG.log(Level.INFO, "Exit code: {0}", res);

        deleteTree((File)brwsr[1]);
    }

    private static void drain(String name, InputStream is) throws IOException {
        int av = is.available();
        if (av > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("v== ").append(name).append(" ==v\n");
            while (av-- > 0) {
                sb.append((char)is.read());
            }
            sb.append("\n^== ").append(name).append(" ==^");
            LOG.log(Level.INFO, sb.toString());
        }
    }

    private void deleteTree(File file) {
        if (file == null) {
            return;
        }
        File[] arr = file.listFiles();
        if (arr != null) {
            for (File s : arr) {
                deleteTree(s);
            }
        }
        file.delete();
    }

    @Override
    public HttpServer call() throws Exception {
        return server;
    }

    @Override
    public synchronized void flush() throws IOException {
        if (flushing != null) {
            return;
        }
        flushing = Thread.currentThread();
        while (flushing == Thread.currentThread()) {
            try {
                wait();
            } catch (InterruptedException ex) {
                LOG.log(Level.FINE, null, ex);
            }
        }
        if (exitCode != 0) {
            throw new IOException("Browser closed with exit code " + exitCode);
        }
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    protected Object[] showBrwsr(URI uri) throws IOException {
        LOG.log(Level.INFO, "Showing {0}", uri);
        if (cmd == null) {
            try {
                LOG.log(Level.INFO, "Trying Desktop.browse on {0} {2} by {1}", new Object[] {
                    System.getProperty("java.vm.name"),
                    System.getProperty("java.vm.vendor"),
                    System.getProperty("java.vm.version"),
                });
                java.awt.Desktop.getDesktop().browse(uri);
                LOG.log(Level.INFO, "Desktop.browse successfully finished");
                return null;
            } catch (UnsupportedOperationException ex) {
                LOG.log(Level.INFO, "Desktop.browse not supported: {0}", ex.getMessage());
                LOG.log(Level.FINE, null, ex);
            } catch (IOException ex) {
                LOG.log(Level.INFO, "Desktop.browse failed: {0}", ex.getMessage());
                LOG.log(Level.FINE, null, ex);
            }
        }
        {
            String cmdName = cmd == null ? "xdg-open" : cmd;
            String[] cmdArr = {
                cmdName, uri.toString()
            };
            LOG.log(Level.INFO, "Launching {0}", Arrays.toString(cmdArr));
            final Process process = Runtime.getRuntime().exec(cmdArr);
            return new Object[] { process, null };
        }
    }

    abstract void generateBck2BrwsrJS(StringBuilder sb, Res loader, String url, boolean unitTestMode) throws IOException;
    abstract String harnessResource();
    Object compileJar(URL jar, URL precompiled) throws IOException {
        return null;
    }
    String compileFromClassPath(URL f, Res loader) throws IOException {
        return null;
    }

    private static URI pageURL(String protocol, HttpServer server, final String page) {
        int port = findPort(server);
        try {
            return new URI(protocol + "://localhost:" + port + page);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int findPort(HttpServer s) {
        NetworkListener listener = s.getListeners().iterator().next();
        int port = listener.getPort();
        return port;
    }

    final class Res {
        private final Set<URL> ignore = new HashSet<>();

        Object compileJar(URL jarURL) throws IOException {
            List<String[]> libraries = new ArrayList<>();
            Map<String,Object[]> osgiJars = new HashMap<>();
            for (ClassLoader loader : loaders) {
                Enumeration<URL> en = loader.getResources("META-INF/MANIFEST.MF");
                while (en.hasMoreElements()) {
                    URL e = en.nextElement();
                    Manifest mf = new Manifest(e.openStream());
                    for (Map.Entry<String, Attributes> entrySet : mf.getEntries().entrySet()) {
                        String key = entrySet.getKey();
                        Attributes attr = entrySet.getValue();

                        final String a = attr.getValue("Bck2BrwsrArtifactId");
                        final String g = attr.getValue("Bck2BrwsrGroupId");
                        final String v = attr.getValue("Bck2BrwsrVersion");
                        final String d = attr.getValue("Bck2BrwsrDebug");
                        final String n = attr.getValue("Bck2BrwsrName");

                        if (g != null && a != null && v != null && "true".equals(d)) {
                            libraries.add(new String[] {
                                a, g, v, key, n
                            });
                        }
                    }
                    final Attributes main = mf.getMainAttributes();
                    String symbol = main.getValue("Bundle-SymbolicName");
                    String version;
                    if (symbol == null) {
                        symbol = main.getValue("OpenIDE-Module-Name");
                        version = main.getValue("OpenIDE-Module-SpecificationVersion");
                    } else {
                        version = main.getValue("Bundle-Version");
                    }
                    if (symbol != null) {
                        osgiJars.put(symbol, new Object[] { e, version });
                    }
                }
            }
            URL precompiled = null;
            for (ClassLoader loader : loaders) {
                for (String[] lib : libraries) {
                    final String res = "META-INF/maven/" + lib[1] + "/" + lib[0] + "/pom.properties";
                    URL props = loader.getResource(res);
                    if (props != null) {
                        URLConnection c = props.openConnection();
                        Properties load = new Properties();
                        try (InputStream is = c.getInputStream()) {
                            load.load(is);
                        }
                        if (lib[2].equals(load.getProperty("version"))) {
                            if (c instanceof JarURLConnection) {
                                final URL definedInURL = ((JarURLConnection)c).getJarFileURL();
                                if (definedInURL.equals(jarURL)) {
                                    precompiled = loader.getResource(lib[3]);
                                }
                            }
                        }
                    }
                }
            }
            if (precompiled == null) {
                for (ClassLoader loader : loaders) {
                    for (String[] lib : libraries) {
                        Object[] urlVersion = osgiJars.get(lib[4]);
                        String expectVersion = lib[2];
                        if (expectVersion.endsWith("-SNAPSHOT")) {
                            expectVersion = expectVersion.substring(0, expectVersion.length() - 9);
                        }
                        if (urlVersion != null && urlVersion[1].toString().startsWith(expectVersion)) {
                            URL manifest = (URL) urlVersion[0];
                            if (manifest.openConnection() instanceof JarURLConnection) {
                                JarURLConnection jarConn = (JarURLConnection) manifest.openConnection();
                                if (jarConn.getJarFileURL().equals(jarURL)) {
                                    precompiled = loader.getResource(lib[3]);
                                }
                            }
                        }
                    }
                }
            }
            Object ret = BaseHTTPLauncher.this.compileJar(jarURL, precompiled);
            ignore.add(jarURL);
            return ret;
        }
        String compileFromClassPath(URL f) throws IOException {
            return BaseHTTPLauncher.this.compileFromClassPath(f, this);
        }
        public URL get(String resource, int skip) throws IOException {
            if (!resource.endsWith(".class")) {
                return getResource(resource, skip);
            }
            URL u = null;
            for (ClassLoader l : loaders) {
                Enumeration<URL> en = l.getResources(resource);
                while (en.hasMoreElements()) {
                    u = en.nextElement();
                    if (u.toExternalForm().matches("^.*emul.*rt\\.jar.*$")) {
                        return u;
                    }
                }
            }
            if (u != null) {
                if (u.toExternalForm().contains("rt.jar")) {
                    LOG.log(Level.WARNING, "No fallback to bootclasspath for {0}", u);
                    return null;
                }
                return u;
            }
            throw new IOException("Can't find " + resource);
        }
        private URL getResource(String resource, int skip) throws IOException {
            for (ClassLoader l : loaders) {
                Enumeration<URL> en = l.getResources(resource);
                while (en.hasMoreElements()) {
                    final URL now = en.nextElement();
                    if (now.toExternalForm().contains("sisu-inject-bean")) {
                        // certainly we don't want this resource, as that
                        // module is not compiled with target 1.6, currently
                        continue;
                    }
                    if (now.getProtocol().equals("jar")) {
                        JarURLConnection juc = (JarURLConnection) now.openConnection();
                        if (now.getFile().endsWith(".class") && ignore.contains(juc.getJarFileURL())) {
                            continue;
                        }
                    }
                    if (--skip < 0) {
                        return now;
                    }
                }
            }
            throw new IOException("Not found (anymore of) " + resource);
        }
    }

    private static class Page extends HttpHandler {
        final String resource;
        private final String[] args;
        private final Res res;
        private final String ensureBck2Brwsr;

        public Page(Res res, String resource, String... args) {
            this(null, res, resource, args);
        }

        Page(String ensureBck2Brwsr, Res res, String resource, String... args) {
            this.ensureBck2Brwsr = ensureBck2Brwsr;
            this.res = res;
            this.resource = resource;
            this.args = args.length == 0 ? new String[] { "$0" } : args;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            String r = computePage(request);
            if (r.startsWith("/")) {
                r = r.substring(1);
            }
            String[] replace = {};
            if (r.endsWith(".html")) {
                response.setContentType("text/html");
                LOG.info("Content type text/html");
                replace = args;
            }
            if (r.endsWith(".xhtml")) {
                response.setContentType("application/xhtml+xml");
                LOG.info("Content type application/xhtml+xml");
                replace = args;
            }
            try {
                InputStream is = res.get(r, 0).openStream();
                if (ensureBck2Brwsr != null && ensureBck2Brwsr.equals(request.getRequestURI())) {
                    ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                    copyStream(is, tmp, request.getRequestURL().toString(), replace);
                    String pageText = tmp.toString("UTF-8");
                    if (!pageText.contains("bck2brwsr.js")) {
                        int last = pageText.toLowerCase().indexOf("</body>");
                        if (last == -1) {
                            last = pageText.length();
                        }
                        pageText = pageText.substring(0, last) +
                            "\n<script src='/bck2brwsr.js'></script>\n\n" +
                            pageText.substring(last);
                    }
                    response.getWriter().write(pageText);
                } else {
                    OutputStream os = response.getOutputStream();
                    copyStream(is, os, request.getRequestURL().toString(), replace);
                }
            } catch (IOException ex) {
                response.setDetailMessage(ex.getLocalizedMessage());
                response.setError();
                response.setStatus(404);
            }
        }

        protected String computePage(Request request) {
            String r = resource;
            if (r == null) {
                r = request.getHttpHandlerPath();
            }
            return r;
        }
    }

    private static class SubTree extends Page {

        public SubTree(Res res, String resource, String... args) {
            super(res, resource, args);
        }
        public SubTree(String ensureBck2Brwsr, Res res, String resource, String... args) {
            super(ensureBck2Brwsr, res, resource, args);
        }

        @Override
        protected String computePage(Request request) {
            return resource + request.getHttpHandlerPath();
        }


    }

    private class Console extends HttpHandler {
        @Override
        public void service(Request request, Response rspns) throws Exception {
            String url = request.getRequestURI();
            String msg = request.getParameter("msg");
            if (url.endsWith("/log/")) {
                OUT.info(msg);
            } else {
                OUT.warning(msg);
            }
            rspns.finish();
        }
    }

    private final class VMAndPages extends StaticHttpHandler {
        private final boolean unitTestMode;
        private String vmResource;

        public VMAndPages(boolean unitTestMode) {
            super((String[]) null);
            this.unitTestMode = unitTestMode;
            this.setFileCacheEnabled(false);
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            final String exit = request.getParameter("exit");
            if (exit != null) {
                int exitCode;
                try {
                    exitCode = Integer.parseInt(exit);
                } catch (NumberFormatException ex) {
                    exitCode = "true".equals(exit) ? 0 : -1;
                }
                if (exitCode != -1) {
                    LOG.info("Exit request received. Shutting down!");
                    shutdown(exitCode, false);
                }
            }
            if (request.getRequestURI().equals(vmResource)) {
                response.setCharacterEncoding("UTF-8");
                response.setContentType("text/javascript");
                StringBuilder sb = new StringBuilder();
                generateBck2BrwsrJS(sb, BaseHTTPLauncher.this.resources, request.getRequestURL().toString(), unitTestMode);
                response.getWriter().write(sb.toString());
            } else {
                super.service(request, response);
            }
        }

        private void registerVM(String vmResource) {
            this.vmResource = vmResource;
        }
    }

    private static class Classes extends HttpHandler {
        private final Res loader;

        public Classes(Res loader) {
            this.loader = loader;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            String res = request.getHttpHandlerPath();
            if (res.startsWith("/")) {
                res = res.substring(1);
            }
            String skip = request.getParameter("skip");
            int skipCnt = skip == null ? 0 : Integer.parseInt(skip);
            URL url = loader.get(res, skipCnt);
            if (url != null && !res.equals("META-INF/MANIFEST.MF")) try {
                response.setCharacterEncoding("UTF-8");
                if (url.getProtocol().equals("jar")) {
                    JarURLConnection juc = (JarURLConnection) url.openConnection();
                    Object s = null;
                    try {
                        s = loader.compileJar(juc.getJarFileURL());
                    } catch (IOException iOException) {
                        throw new IOException("Can't compile " + url.toExternalForm(), iOException);
                    }
                    if (s instanceof String) {
                        try (Writer w = response.getWriter()) {
                            w.append((String)s);
                        }
                        return;
                    }
                    if (s instanceof InputStream) {
                        copyStream((InputStream) s, response.getOutputStream(), null);
                        return;
                    }
                }
                if (url.getProtocol().equals("file")) {
                    final String filePart = url.getFile();
                    if (filePart.endsWith(res)) {
                        url = new URL(
                            url.getProtocol(),
                            url.getHost(),
                            url.getPort(),
                            filePart.substring(0, filePart.length() - res.length())
                        );
                    }
                    String s = loader.compileFromClassPath(url);
                    if (s != null) {
                        try (Writer w = response.getWriter()) {
                            w.append(s);
                        }
                        return;
                    }
                }
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Cannot handle " + res, ex);
            }
            InputStream is = null;
            try {
                if (url == null) {
                    throw new IOException("Resource not found");
                }
                is = url.openStream();
                response.setContentType("text/javascript");
                Writer w = response.getWriter();
                w.append("([");
                for (int i = 0;; i++) {
                    int b = is.read();
                    if (b == -1) {
                        break;
                    }
                    if (i > 0) {
                        w.append(", ");
                    }
                    if (i % 20 == 0) {
                        w.write("\n");
                    }
                    if (b > 127) {
                        b = b - 256;
                    }
                    w.append(Integer.toString(b));
                }
                w.append("\n])");
            } catch (IOException ex) {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                response.setError();
                response.setDetailMessage(ex.getMessage());
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

    }
    private static class WS extends WebSocketApplication {

        private final Resource r;

        private WS(Resource r) {
            this.r = r;
        }

        @Override
        public void onMessage(WebSocket socket, String text) {
            try {
                r.httpContent.reset();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                copyStream(r.httpContent, out, null, text);
                String s = new String(out.toByteArray(), "UTF-8");
                socket.send(s);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, null, ex);
            }
        }
    }

    private static class LifeCycleApp extends WebSocketApplication {
        private final Timer countDown = new Timer("Server countdown");
        private final BaseHTTPLauncher server;
        private final ReloadHandler reloadHandler;
        private final Logger log;
        private final int countDownTime;
        private final Map<WebSocket,Object> activeClients = new WeakHashMap<>();
        private TimerTask countDownTask;

        public LifeCycleApp(BaseHTTPLauncher s, int timeout, Logger log) {
            this.server = s;
            this.log = log;
            this.countDownTime = timeout;
            this.reloadHandler = new ReloadHandler();
        }

        @Override
        protected boolean onError(WebSocket webSocket, Throwable t) {
            log.log(Level.WARNING, null, t);
            return true;
        }

        @Override
        public void onConnect(WebSocket socket) {
            newCountDownTask(null);
            activeClients.put(socket, this);
            log.log(Level.INFO, "Connected client count: #{0}", activeClients.size());
        }

        @Override
        public void onClose(WebSocket socket, DataFrame frame) {
            activeClients.remove(socket);
            if (activeClients.isEmpty()) {
                log.info("Last client disconnected. Final countdown.");
                long then = System.currentTimeMillis();
                TimerTask newTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (!activeClients.isEmpty()) {
                            return;
                        }

                        long now = System.currentTimeMillis();
                        int delay = (int) Math.round((now - then) / 1000.0);
                        log.log(Level.INFO, "No client for {0} seconds. Shutting down.", delay);
                        try {
                            server.shutdown(0, false);
                        } catch (IOException ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                    }
                };
                newCountDownTask(newTask);
                return;
            }
            log.log(Level.INFO, "Client disconnected. {0} client(s) remaining", activeClients.size());
        }

        @Override
        public void onPong(WebSocket socket, byte[] bytes) {
        }

        @Override
        public void onPing(WebSocket socket, byte[] bytes) {
        }

        @Override
        public void onMessage(WebSocket socket, byte[] bytes) {
        }

        @Override
        public void onMessage(WebSocket socket, String text) {
            if (text.startsWith("exit:")) {
                String exit = text.substring(5).trim();
                int exitCode;
                try {
                    exitCode = Integer.parseInt(exit);
                } catch (NumberFormatException ex) {
                    exitCode = "true".equals(exit) ? 0 : -1;
                }
                if (exitCode != -1) {
                    log.info("Exit request received. Shutting down!");
                    try {
                        server.shutdown(exitCode, false);
                    } catch (IOException ex) {
                        log.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        private synchronized void newCountDownTask(TimerTask newTask) {
            TimerTask prevTask = countDownTask;
            if (prevTask != null) {
                prevTask.cancel();
            }
            countDownTask = newTask;
            if (newTask != null) {
                countDown.schedule(newTask, countDownTime);
            }
        }

        public HttpHandler getReloadHandler() {
            return reloadHandler;
        }

        private class ReloadHandler extends HttpHandler {
            @Override
            public void service(Request rqst, Response rspns) throws Exception {
                log.log(Level.INFO, "Request to reload {0} with {1} active clients.", new Object[] { rqst.getRequestURI(), activeClients.keySet().size() });
                try (Writer w = rspns.getWriter()) {
                    if (activeClients.isEmpty()) {
                        w.write("No client to reload");
                        rspns.setStatus(HttpStatus.NOT_FOUND_404);
                    } else {
                        for (WebSocket s : activeClients.keySet()) {
                            s.send("reload");
                        }
                        w.write("Reloaded");
                        rspns.setStatus(HttpStatus.OK_200);
                    }
                }
            }
        }
    }
}
