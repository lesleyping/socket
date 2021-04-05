package main.java.aio_chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    private List<ClientHandler> connetedClients;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        this.connetedClients = new ArrayList<>();
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
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

    private void start() {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverChannel.bind(new InetSocketAddress(LOCALHOST, port));
            System.out.println("启动服务器，监听端口：" + port);

            while (true) {
                serverChannel.accept(null, new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }

    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }
            if (clientChannel != null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
                // 将新用户添加到在线用户列表
                addClient(handler);
                clientChannel.read(buffer, buffer, handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败：" + exc);
        }
    }

    private class ClientHandler implements CompletionHandler<Integer, Object> {

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel channel) {
            this.clientChannel = channel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            // 只有read 有attachment
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer != null) {
                if (result <= 0) {
                    // 客户端异常
                    // 将客户移除出在线客户列表
                    removeClient(this);
                } else {
                    buffer.flip();
                    String fwdMsg = receive(buffer);
                    System.out.println(getClientName(clientChannel) + fwdMsg);
                    forwardMessage(clientChannel, fwdMsg);
                    buffer.clear();

                    // 检查用户是否已经退出
                    if (readyToQuit(fwdMsg)) {
                        // 将客户移除出在线客户列表
                        removeClient(this);
                    } else {
                        clientChannel.read(buffer, buffer, this);
                    }
                }
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    private String receive(ByteBuffer buffer) {
        CharBuffer charBuffer = charset.decode(buffer);
        return String.valueOf(charBuffer);
    }

    private String getClientName(AsynchronousSocketChannel channel) {
        int clientPort = -1;
        try {
            InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
            clientPort = address.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端[" + clientPort + "]";
    }

    private synchronized void forwardMessage(AsynchronousSocketChannel channel, String fwdMsg) {
        for (ClientHandler handler : connetedClients) {
            if (!channel.equals(handler.clientChannel)) {
                try {
                    ByteBuffer buffer = charset.encode(getClientName(handler.clientChannel) + ":" + fwdMsg);
                    handler.clientChannel.write(buffer, null, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void addClient(ClientHandler handler) {
        connetedClients.add(handler);
        System.out.println(getClientName(handler.clientChannel) + "已连接服务器");
    }

    private synchronized void removeClient(ClientHandler handler) {
        connetedClients.remove(handler);
        System.out.println(getClientName(handler.clientChannel) + "已断开连接");
        close(handler.clientChannel);
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(7777);
        server.start();
    }
}
