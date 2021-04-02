package main.java.aio_tutorial;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

public class Client {
    private String LOCALHOST = "localhost";
    private int DEFAULT_PORT = 8888;
    AsynchronousSocketChannel clientChannel;

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {

    }
}
