package nio_chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author lixiping <lixiping@kuaishou.com>
 * Created on 2021-03-31
 */
public class ChatServer {
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private ServerSocketChannel server;
    private Selector selector;
    // 读取通道的数据（写进buffer）
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    // 写入客户端的通道（读buffer）
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while(client.read(rBuffer) > 0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private void forwardMessage(SocketChannel client, String msg) throws IOException {
        // 所有注册的集合
        for(SelectionKey key : selector.keys()) {
            Channel connectedClient = (SocketChannel) key.channel();
            if (connectedClient instanceof ServerSocketChannel) {
                continue;
            }
            if (key.isValid() && !client.equals(connectedClient)) {
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(client) + msg));
                // 写反转为读
                wBuffer.flip();
                while(wBuffer.hasRemaining()) {
                    ((SocketChannel) connectedClient).write(wBuffer);
                }
            }
        }
    }

    private void handles(SelectionKey key) throws IOException {
        // ACCEPT 事件 - 和客户端建立了连接
        if (key.isAcceptable()) {
            SocketChannel client = (SocketChannel) key.channel();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) + "已连接");
        }
        // READ 事件 - 客户端发送消息
        else if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            // 从通道里读数据
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                // 客户端异常
                key.cancel();
                selector.wakeup();
            } else {
                forwardMessage(client, fwdMsg);
                if (readyToQuit(fwdMsg)) {
                    key.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client)+ "已断开");
                }
            }
        }
    }

    private String getClientName(SocketChannel client) {
        return "客户端[" + client.socket().getPort() + "]";
    }

    private void start() {
        try {
            // 默认"阻塞式"
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + port + "...");

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // 处理被触发的事件
                    handles(key);
                }
                // 清除过期事件
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);
        }
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }
}
