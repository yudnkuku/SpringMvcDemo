# Memory Analysis Tool

标签（空格分隔）： MAT

---

## Shallow Heap & Retained Heap ##
在`MAT`中定义了两种类型的对象大小：`Shallow Heap`和`Retained Heap`，首先看一张引用关系图：

![对象引用关系][1]

上面定义了很多对象，其中的引用关系可以描述为：

 - 对象`A`持有对象`B`和`C`的引用
 - 对象`B`持有对象`D`和`E`的引用
 - 对象`C`持有对象`F`和`G`的引用

假设每个对象占用10字节的内存。

**Shallow Heap**

对象的`Shallow heap`大小就是对象占用内存的大小，对上面的所有对象，它们的`Shallow heap`大小都一样，都是10字节

**Retained Heap**

对于上述对象的引用关系而言，对象`B`持有`D`和`E`的引用，所以当`B`被回收时，将不会有活跃的引用指向对象`D`和`E`，这意味着`D`和`E`也会被回收，`Retained Heap`指的是某个对象被回收时释放的内存空间，因此对于对象`B`，其`Retained Heap`大小是：`shallow(B)+shallow(D)+shallow(E)=30bytes`，同理对于杜希昂`C`，其`Retained Heap`大小也为30字节，对象`A`为70字节。

![Retained Heap][2]

再来看一张图，假设增加一个对象`H`，持有对象`B`的引用：

![Retained Heap 2][3]

此时，如果对象`A`被回收，那么`H`仍会持有对象`B`的应用，`B`不会被回收，则`A`的`Retained Heap`大小等于40字节(`A+C+F+G`)

## Incoming reference & Outgoing reference ##
在`MAT`中查看对象引用关系：

    ![incoming_outgoing_reference][4]
    
这两个定义实际上也比较简单：

 - `incoming reference`指的是持有该对象引用的对象，例如`A`对象的`incoming reference`指的就是所有持有对象`A`引用的对象

如下图所示，`DefaultSqlSessionFactory`对象就被其他很多杜希昂引用，因此它的`incoming reference`一一列出：

![incoming_reference][5]

 - `outgoing reference`指的是该对象持有的其他对象的引用，与上面引用方向刚好相反。

![outgoing_reference][6]


 
 


  [1]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/Shallow-heap-1.png
  [2]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/shallow-heap-2-1.png
  [3]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/Shallow-heap-3-1.png
  [4]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/incoming_outgoing_references.png
  [5]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/incoming_reference.jpg
  [6]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/outgoing_reference.jpg