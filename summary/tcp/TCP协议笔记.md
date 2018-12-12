# TCP协议笔记

标签（空格分隔）： 计算机网路

---

## 1、TCP状态 ##
## LISTENING ##
服务端状态，侦听来自远方的`TCP`端口的连接请求。
有某种服务才会处于`LSITENING`状态，`TCP`状态就是某个端口的状态变化，提供一个服务就打开一个端口，例如常见的`http 80`端口，`ftp 21`端口等。

## SYN-SENT ##
**客户端**`SYN-SENT`状态，当请求连接时客户端首先要发送同步信号给要访问的机器，此时客户端的状态为`SYN-SENT`，正常情况下，`SYN-SENT`状态非常短暂，如果连接建立成功，状态立刻变为`ESTABLISHED`

## SYN-RECEIVED ##
**服务端**`SYN-RCVD`状态，当服务端收到客户端发送的同步信号时，将标志位`ACK`和`SYN`发送给客户端，此时服务器端处于`SYN-RCVD`状态

## ESTABLISHED ##
**客户端和服务端**状态，表示连接已经建立，可以相互传送数据

## FIN-WAIT-1 ##
主动关闭端应用程序调用`close`，于是其`TCP`发出`FIN`请求(向服务端发送`FIN`包)主动关闭连接，之后进入`FIN_WAIT_1`状态

## CLOSE-WAIT ##
被动关闭端`TCP`接收到`FIN`包之后，就发出`ACK`以回应`FIN`请求(这个`FIN`请求的接收也作为文件结束符传递给上层应用程序，一段时间后，导致被动关闭端应用程序调用`close`关闭连接，就进入`LAST-ACK`状态)，进入`CLOSE_WAIT`状态。

## FIN-WAIT-2 ##
主动关闭端接到被动关闭端返回的`ACK`后，就进入了`FIN-WAIT-2`状态，这是著名的半关闭状态，这是在关闭连接时，客户端和服务端两次握手之后的状态，在这个状态下，应用程序还有接受数据的能力，但已经无法发送数据。

## LAST-ACK ##
**服务端**状态，被动关闭端一段时间之后，接收到文件结束符的应用程序将调用`close`关闭连接，这导致它的`TCP`也发送一个`FIN`，等待对方的`ACK`，就进入了`LAST-ACK`状态。

## TIME-WAIT ##
**客户端**状态，在主动关闭端收到`FIN`包之后，`TCP`就会发送`ACK`包，并进入`TIME-WAIT`状态

## CLOSED ##
被动关闭端在接收到`ACK`包之后，就进入了`CLOSED`状态，连接结束

![TCP握手][1]
## 2、TCP状态迁移 ##
状态迁移如下图所示：

![TCP状态迁移][2]

## 客户端应用程序状态迁移图 ##
客户端的状态可以用如下的流程来表示：

    CLOSED->SYN_SENT->ESTABLISHED->FIN_WAIT_1->FIN_WAIT_2->TIME_WAIT->CLOSED
    
## 服务端应用程序的状态迁移图 ##
流程如下：

    CLOSED->LISTEN->SYN_RCVD->ESTABLISHED->CLOSE_WAIT->LAST_ACK->CLOSED
    
## 其他状态迁移 ##
还有一些其它的状态迁移，总结如下：
`LISTEN->SYN_SENT`，服务器有时候也需要打开连接
`SYN_SENT->SYN`，服务端和客户端在`SYN_SENT`状态下如果收到`SYN`数据报，则都需要发送`SYN`的`ACK`数据报并把自己的状态调整到`SYN_RCVD`状态。准备进入`ESTABLISHED`
`SYN_SENT->CLOSED`，在发送超时情况下会返回到`CLOSED`状态
`SYN_RCVD->LISTEN`，如果收到`RST`包，则返回到`LISTEN`状态
`SYN_RCVD->FIN_WAIT_1`，这个迁移是说可以不用到`ESTABLISHED`状态，而可以直接跳转到`FIN_WAIT_1`状态并等待关闭。

## 3、TCP连接建立三次握手 ##
`TCP`是面向连接的协议，双方在每次发送数据之前，都需要建立一条连接。
`Client`连接`Server`：
当`Client`端调用`socket`函数时，相当于`Client`端产生了一个处于`CLOSED`状态的套接字。
(1)第一次握手：`Client`端调用`connect()`函数，系统为`Client`随机分配一个端口，连同传入的参数(`server`的`ip`和端口)组成一个连接四元组，`client`发送一个带`SYN`标志的`TCP`报文(`SYN J`)到`server`，等待服务器确认。
(2)第二次握手：服务器收到`SYN`包，必须确认客户的`SYN`(`ACK J+1`)，同时自己也发送一个`SYN`包(`SYN K`)，即`SYN+ACK`包，此时服务器进入`SYN_RCVD`状态。
(3)第三次握手：`Client`收到`SYN+ACK`包，向`server`发送`ACK`包(`ACK K+1`)，发送完毕后，`Client&Server`均进入`ESTABLISHED`状态，完成三次握手，连接可以进行读写操作。

![TCP三次握手][3]

## 4、TCP连接终止(四次握手释放) ##
由于`TCP`连接时全双工的，因此每个方向都必须单独进行关闭，原则上时当一方完成它的数据发送任务后就能发送一个`FIN`来终止这个方向的连接，收到一个`FIN`只意味着这一方向上没有数据流动，一个`TCP`连接在收到一个`FIN`后仍能发送数据。首先进行关闭的一方将执行主动关闭，而另一方执行被动关闭。而释放连接需要进行四次握手。

![TCP释放连接四次握手][4]

上述过程描述为：
(1)主动关闭连接端`host A`发送一个`FIN`请求，用来关闭`A`到`B`的数据传送，`A`状态是`FIN_WAIT_1`,`B`在收到`FIN`包后状态变成`CLOSE_WAIT`
(2)被动关闭端`host B`收到这个`FIN`包，响应一个`ACK`包，确认序号为收到的序号加1，`A`在收到`ACK`包之后状态变成`FIN_WAIT_2`
(3)`B`关闭和`A`的连接，发送一个`FIN`给`A`，`B`状态变成`LAST_ACK`，`A`在收到`FIN`包之后状态变成`TIME_WAIT`
(4)随后`A`发送一个`ACK`包给`B`，此时`A`和`B`状态均会变成`CLOSED`，通道关闭

![此处输入图片的描述][5]

## 5、TCP的FLAGS说明 ##
在`TCP`层，有个`FLAGS`字段，这个字段有以下几个标识：`SYN/FIN/ACK/PSH/RST/URG`

(1)`SYN`字段标识建立连接
该标志仅在三次握手建立`TCP`连接时有效，它提示`TCP`连接的服务端检查序列编号，该序列编号为`TCP`连接初始端(一般是客户端)的初始序列编号

(2)`FIN`表示关闭连接
(3)`ACK`表示响应
确认编号提示远端系统已经成功接收所有数据

  [1]: http://dl.iteye.com/upload/attachment/366894/b6f5e19d-5c3a-38e6-ac70-00ff44a92621.jpg
  [2]: http://dl.iteye.com/upload/attachment/365267/42e653f4-27d2-3025-9d77-23ab92df316e.jpg
  [3]: http://my.csdn.net/uploads/201204/10/1334045728_5744.png
  [4]: http://my.csdn.net/uploads/201204/10/1334046363_4881.jpg
  [5]: http://my.csdn.net/uploads/201204/10/1334046534_7834.png