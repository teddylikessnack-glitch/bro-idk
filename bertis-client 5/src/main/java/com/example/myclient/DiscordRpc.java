package com.example.myclient;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Discord Rich Presence. Needs the Discord desktop app to be running.
// SETUP: create an app at https://discord.com/developers/applications named "Bertis Client",
// paste its Application ID below, and upload the logo under Rich Presence > Art Assets as "logo".
public final class DiscordRpc {
    private static final String CLIENT_ID = "1573796694720782487";
    private static final String LOGO_KEY = "logo";
    private static final long MIN_INTERVAL_MS = 15_000; // Discord rate limit

    private interface Conn {
        void write(byte[] b) throws IOException;
        void readFully(byte[] b) throws IOException;
        void close();
    }

    private static final class FileConn implements Conn {
        private final RandomAccessFile f;
        FileConn(RandomAccessFile f) { this.f = f; }
        public void write(byte[] b) throws IOException { f.write(b); }
        public void readFully(byte[] b) throws IOException { f.readFully(b); }
        public void close() { try { f.close(); } catch (IOException ignored) { } }
    }

    private static final class SockConn implements Conn {
        private final SocketChannel c;
        SockConn(SocketChannel c) { this.c = c; }
        public void write(byte[] b) throws IOException {
            ByteBuffer bb = ByteBuffer.wrap(b);
            while (bb.hasRemaining()) c.write(bb);
        }
        public void readFully(byte[] b) throws IOException {
            ByteBuffer bb = ByteBuffer.wrap(b);
            while (bb.hasRemaining()) if (c.read(bb) < 0) throw new IOException("closed");
        }
        public void close() { try { c.close(); } catch (IOException ignored) { } }
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Bertis-RPC");
        t.setDaemon(true);
        return t;
    });

    private static volatile Conn conn;
    private static volatile String lastDetails = null;
    private static long lastSend = 0;
    private static final long START = System.currentTimeMillis() / 1000;

    private DiscordRpc() {}

    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(DiscordRpc::disconnect));
    }

    // Call every client tick.
    public static void tick(Minecraft mc) {
        if (CLIENT_ID.startsWith("PUT_")) return;

        if (!Settings.discordRpc) {
            if (conn != null) EXEC.execute(DiscordRpc::disconnect);
            lastDetails = null;
            return;
        }

        String details = mc.level != null ? "In a world" : "In the menus";
        long now = System.currentTimeMillis();
        if (details.equals(lastDetails)) return;
        if (now - lastSend < MIN_INTERVAL_MS) return;
        lastSend = now;
        lastDetails = details;
        EXEC.execute(() -> push(details));
    }

    private static void push(String details) {
        try {
            if (conn == null) {
                Conn c = connect();
                if (c == null) {
                    lastDetails = null; // retry later
                    return;
                }
                send(c, 0, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");
                read(c);
                conn = c;
            }
            String json = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                    + ",\"activity\":{\"details\":\"" + details + "\",\"state\":\"Minecraft 26.2\","
                    + "\"timestamps\":{\"start\":" + START + "},"
                    + "\"assets\":{\"large_image\":\"" + LOGO_KEY
                    + "\",\"large_text\":\"Bertis Client\"}}},"
                    + "\"nonce\":\"" + UUID.randomUUID() + "\"}";
            send(conn, 1, json);
            read(conn);
        } catch (Exception e) {
            disconnect();
            lastDetails = null; // retry later
        }
    }

    private static void disconnect() {
        Conn c = conn;
        conn = null;
        if (c != null) c.close();
    }

    private static void send(Conn c, int op, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(op).putInt(data.length).put(data);
        c.write(bb.array());
    }

    private static String read(Conn c) throws IOException {
        byte[] h = new byte[8];
        c.readFully(h);
        ByteBuffer hb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
        hb.getInt();
        int len = hb.getInt();
        if (len < 0 || len > (1 << 20)) throw new IOException("bad frame");
        byte[] d = new byte[len];
        c.readFully(d);
        return new String(d, StandardCharsets.UTF_8);
    }

    private static List<String> unixDirs() {
        List<String> dirs = new ArrayList<>();
        String xdg = System.getenv("XDG_RUNTIME_DIR");
        if (xdg != null) {
            dirs.add(xdg);
            dirs.add(xdg + "/app/com.discordapp.Discord");
            dirs.add(xdg + "/snap.discord");
        }
        for (String k : new String[]{"TMPDIR", "TMP", "TEMP"}) {
            String v = System.getenv(k);
            if (v != null) dirs.add(v);
        }
        dirs.add("/tmp");
        return dirs;
    }

    private static Conn connect() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        for (int i = 0; i < 10; i++) {
            if (windows) {
                try {
                    return new FileConn(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
                } catch (Exception ignored) {
                }
            } else {
                for (String dir : unixDirs()) {
                    try {
                        Path p = Path.of(dir, "discord-ipc-" + i);
                        if (!Files.exists(p)) continue;
                        SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                        ch.connect(UnixDomainSocketAddress.of(p));
                        return new SockConn(ch);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }
}
