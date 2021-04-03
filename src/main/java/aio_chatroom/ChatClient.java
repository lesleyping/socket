package main.java.aio_chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;

public class ChatClient {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private String host;
    private int port;
    private AsynchronousSocketChannel clientChannel;
    private Charset charset = Charset.forName("UTF-8");

    public ChatClient() {
        this(LOCALHOST, DEFAULT_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void start() {

    }

    public void send(String msg) {

    }



    public static void main(String[] args) {

    }
}
