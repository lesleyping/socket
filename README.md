# socket
## BIO 中的阻塞
- ServerSocket.accept()
- InputStream.read(),OutputStream.write()
- 无法在同一个线程中处理多个Stream I/O

## NIO
- 使用Channel 代替Stream
    - Channel 是双向的
    - Channel 提供非阻塞读写方法
- 使用Selector 监控多条Channel
- 可以在一个线程中处理多个Stream I/O
    - context swifting 上下文切换
    - 线程占用系统资源
    
 - 几个重要的Channel
    - FileChannel
    - ServerSocketChannel
    - SocketChannel