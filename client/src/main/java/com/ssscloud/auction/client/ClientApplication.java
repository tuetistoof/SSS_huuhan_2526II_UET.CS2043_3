package com.ssscloud.auction.client;

import java.io.File;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;

import com.ssscloud.auction.client.networking.AuctionClientSocket;
import com.ssscloud.auction.client.util.SceneManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ClientApplication extends Application {
    private static Process localServerProcess = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Properties clientProps = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/client.properties")) {
            if (is != null) clientProps.load(is);
        }

        String host = clientProps.getProperty("server.host", "localhost");
        int    port = Integer.parseInt(clientProps.getProperty("server.port", "5000"));

        boolean connected = canConnect(host, port);

        if (!connected) {
            System.out.println("[INFO] Can't connect to " + host + ":" + port
                    + " — try starting server local...");
            startLocalServer(clientProps);

            host = "localhost";

            connected = waitForServer(host, port, 15_000);
            if (!connected) {
                System.err.println("[ERROR] Server local can not start in 15 seconds. Abortting.");
                System.exit(1);
            }
            System.out.println("[INFO] Server local is ready at localhost:" + port);
        } else {
            System.out.println("[INFO] Connecting to server " + host + ":" + port + " Success.");
        }

        AuctionClientSocket.getInstance().connect(host, port);

        SceneManager.loginScene    = FXMLLoader.load(getClass().getResource("/fxml/login-signup.fxml"));
        SceneManager.registerScene = FXMLLoader.load(getClass().getResource("/fxml/signup.fxml"));

        Scene scene = new Scene(SceneManager.loginScene);
        primaryStage.setTitle("CloudBid");
        Image icon = new Image(getClass().getResourceAsStream("/images/Asset2.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> stopLocalServer());
    }

    /**
     * Thử mở một TCP connection tới host:port trong vòng 2 giây.
     * Trả về true nếu kết nối thành công.
     */
    private boolean canConnect(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tìm file server.jar (cùng thư mục với client.jar, hoặc thư mục hiện tại)
     * rồi khởi động nó như một process con.
     */
    private void startLocalServer(Properties props) {
        File serverJar = findServerJar();
        if (serverJar == null || !serverJar.exists()) {
            System.err.println("[WARN] Can not find server.jar — proceeding with direct connection...");
            return;
        }

        String dbUrl  = props.getProperty("local.db.url",
                "jdbc:mysql://localhost:3306/cloud?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
        String dbUser = props.getProperty("local.db.username", "root");
        String dbPass = props.getProperty("local.db.password", "");

        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", serverJar.getAbsolutePath());
            pb.environment().put("SPRING_DATASOURCE_URL",      dbUrl);
            pb.environment().put("SPRING_DATASOURCE_USERNAME", dbUser);
            pb.environment().put("SPRING_DATASOURCE_PASSWORD", dbPass);
            pb.redirectOutput(new File("server-local.log"));
            pb.redirectError(new File("server-local.log"));
            localServerProcess = pb.start();
            System.out.println("[INFO] Start server local (PID=" + localServerProcess.pid() + ")");
        } catch (Exception e) {
            System.err.println("[ERROR] Can not start server.jar: " + e.getMessage());
        }
    }

    private File findServerJar() {
        try {
            File clientJar = new File(
                getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            File sibling = new File(clientJar.getParentFile(), "server.jar");
            if (sibling.exists()) return sibling;
        } catch (Exception ignored) {}

        File cwd = new File("server.jar");
        if (cwd.exists()) return cwd;

        return null;
    }

    /**
     * Poll đến khi server chấp nhận kết nối hoặc hết timeoutMs.
     */
    private boolean waitForServer(String host, int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (canConnect(host, port)) return true;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    private void stopLocalServer() {
        if (localServerProcess != null && localServerProcess.isAlive()) {
            localServerProcess.destroy();
            System.out.println("[INFO] Server local was shut down.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}