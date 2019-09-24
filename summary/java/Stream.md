# Stream

标签（空格分隔）： java8

---

## 如何使用Stream ##
聚合操作是`Java8`针对集合类，使编程更为便利的方式，可以与`Lambda`表达式一起用，达到更加简洁的目的。

对聚合操作的使用可以归结为3部分：

 - 创建`Stream`：通过集合的`stream()`方法，取得集合对象的数据集
 - `Intermediate`：通过一系列中间方法，对数据集进行过滤、检索等数据集的再次处理，如使用`filter`方法对数据进行过滤
 - `Terminal`：通过最终方法完成对数据集中元素的处理，如使用`forEach()`完成对过滤后数据的打印

在一次聚合操作中，可以有多个`Intermediate`，但是有且只有一个`Terminal`。

## 创建Stream ##
有多种方式可以生成`Stream`：

 - `Stream`接口的静态工厂方法(注意：`Java8`接口可以带静态方法)
 - `Collection`接口和数组的默认方法，把一个`Collection`对象转换成`Stream`
 - 其他如：`Random.ints()/BitSet.stream()`等

**Stream静态工厂方法**

1、`of`方法，生成有限长度的`Stream`，长度为参数元素的个数

    of(T.. values):返回多个T元素的Stream
    of(T t):返回只含有一个元素的的Stream
    
示例：

    Stream<Integer> istream = Stream.of(1,2,3);
    Stream<String> sstream = Stream.of("A");
    
2、`generate`方法，返回一个无限长度的`Stream`，其元素由`Supplier`接口提供，`Supplier`是一个函数接口，只有一个方法`get`，用来返回任何泛型的值：

    generate(Supplier<T> s)：返回一个无限长度的Stream
    
示例：

    Stream<Double> dblStream = Stream.generate(() -> Math::random);
    
3、`iterate`方法，其返回的是一个无限长度的`Stream`，与`generate`方法不同的是，其通过函数`f`迭代对指定的元素种子产生无限连续有序`Stream`，其中包含的元素可以认为是`seed,f(seed),f(f(seed))`无限循环

    iterate(final T seed, final UnaryOperator<T> f)
    
示例：

    Stream.iterate(1, item -> item + 1)
        .limit(10)
        .forEach(System.out::println); 
        // 打印结果：1，2，3，4，5，6，7，8，9，10
        
该`Stream`是无限长度的，应该用`filter`或者`limit`来截断`Stream`，避免一直循环下去

3、`empty`方法返回一个空的顺序`Stream`，该`Stream`里面不包含任何元素项

## Collection接口和数组默认方法 ##
在`Collection`接口中，定义了一个默认方法`stream()`，用来生成一个`Stream`

    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
    
在`Arrays`类中，封装了一些`Stream`方法，不仅针对任何类型的元素采用了泛型，也对基本类型做了相应的封装

    public class Arrays {
        ***
        public static <T> Stream<T> stream(T[] array) {
            return stream(array, 0, array.length);
        }
    
       public static LongStream stream(long[] array) {
            return stream(array, 0, array.length);
        }
        ***
    ｝

示例：

    int ids[] = new int[]{1, 2, 3, 4};
    Arrays.stream(ids)
            .forEach(System.out::println);

## Intermediate ##
`Intermediate`主要是用来对`Stream`做出相应转换及限制流，实际上是将源`Stream`转换成一个新的`Stream`，以达到需求效果

**concat**

`concat`方法将两个`Stream`连接到一起，合成一个`Stream`，若两个输入的`Stream`都是排序的，则新`Stream`也是排序的，若输入的`Stream`中任何一个是并行的，则新的`Stream`也是并行的，若关闭新的`Stream`，则原来的两个`Stream`都将执行关闭处理。

示例：

    Stream.concat(Stream.of(1, 2, 3), Stream.of(4, 5))
           .forEach(integer -> System.out.print(integer + "  "));
    // 打印结果
    // 1  2  3  4  5  
    
**distinct**

`distinct`方法已达到去除掉原`Stream`中重复的元素，生成的新`Stream`中没有重复的元素。

示例：

    Stream.of(1,2,3,1,2,3)
        .distinct()
        .forEach(System.out::println); // 打印结果：1，2，3
        
**filter**

`filter`方法对原`Stream`按照指定条件过滤，在新建的`Stream`中，只包含满足条件的元素，将不满足条件的元素过滤掉。

示例：

    Stream.of(1, 2, 3, 4, 5)
        .filter(item -> item > 3)
        .forEach(System.out::println);
        // 打印结果：4，5
        
**map**

`map`方法将对于`Stream`中包含的元素使用给定的转换函数进行转换操作，新生成的`Stream`只包含转换生成的元素，为了提高处理效率，官方已经封装好了三种变形：`mapToDouble/mapToInt/mapToLong`

示例：

    Stream.of("a", "b", "hello")
        .map(item-> item.toUpperCase())
        .forEach(System.out::println);
        // 打印结果
        // A, B, HELLO

**flatMap**

`flatMap`方法与`map`方法类似，都是将原`Stream`中的每一个元素通过转换函数转换，不同的是该转换函数的对象是一个`Stream`，也不会再创建新的`Stream`，而是将原`Stream`的元素取代为转换的`Stream`

示例：

    Stream.of(1, 2, 3)
    .flatMap(integer -> Stream.of(integer * 10))
    .forEach(System.out::println);
    // 打印结果
    // 10，20，30
    
**peek**

`peek`方法生成一个包含原`Stream`的所有元素的新`Stream`，同时会提供一个消费函数`Consumer`实例，新的`Stream`每个元素被消费时都会执行给定的消费函数，并且消费函数优先执行

示例：

    Stream.of(1, 2, 3, 4, 5)
            .peek(integer -> System.out.println("accept:" + integer))
            .forEach(System.out::println);
    // 打印结果
    // accept:1
    //  1
    //  accept:2
    //  2
    //  accept:3
    //  3
    //  accept:4
    //  4
    //  accept:5
    //  5

**skip**

`skip`方法将过滤掉`Stream`中的前`N`个元素。

示例：

    Stream.of(1, 2, 3,4,5)
    .skip(2)
    .forEach(System.out::println);
    // 打印结果
    // 3,4,5
    
**sorted**

`sorted`方法将对原`Stream`进行排序，返回一个有序的新`Stream`，`sorted`接受一个`Comparator`函数作为自定义的比较函数

示例：

    Stream.of(5, 4, 3, 2, 1)
        .sorted()
        .forEach(System.out::println);
        // 打印结果
        // 1，2，3,4,5
        
## Terminal ##
**collect**

`collect`使用聚合器(`Collector`)将流聚合成一个新的集合，方法原型：

    <R, A> R collect(Collector<? super T, A, R> collector);
    
例如最简单的将`Stream`聚合成列表`List`：

    List<Integer> list = Stream.of(1,2,3).collect(Collectors.toList());
    
`Collectors`提供了很多有用的生成`Collector`的静态方法，例如：

 - 列表：`Collectors.toList()`
 - `TreeSet`：`Collectors.toCollection(TreeSet::new)`
 - 连接：`Collectors.joining()`
 - 求和：`Collectors.summingInt/summingDouble/summingLong`
 - 分组：`Collectors.groupingBy()`
 - 分区：`Collectors.partitioningBy()`

下面是文档中给出的示例：

1、将`Stream`聚合成列表

    // Accumulate names into a List
    List<String> list = people.stream().map(Person::getName).collect(Collectors.toList());

2、将`Stream`聚合成`TreeSet`

    // Accumulate names into a TreeSet
    Set<String> set = people.stream().map(Person::getName).collect(Collectors.toCollection(TreeSet::new));

3、将元素转换为字符串并将其用逗号连接

    // Convert elements to strings and concatenate them, separated by commas
    String joined = things.stream().map(Object::toString).collect(Collectors.joining(", "));
    
4、计算员工的工资总和

    // Compute sum of salaries of employee
    int total = employees.stream().collect(Collectors.summingInt(Employee::getSalary)));
    
5、将员工按部门分组

        // Group employees by department
        Map<Department, List<Employee>> byDept
                = employees.stream()
                            .collect(Collectors.groupingBy(Employee::getDepartment));

6、按部门计算员工工资

        // Compute sum of salaries by department
        Map<Department, Integer> totalByDept
                = employees.stream()
                            .collect(Collectors.groupingBy(Employee::getDepartment,
                                                   Collectors.summingInt(Employee::getSalary)));
     
7、将学生按照是否及格分区

        // Partition students into passing and failing
        Map<Boolean, List<Student>> passingFailing 
                = students.stream()
                            .collect(Collectors.partitioningBy(s -> s.getGrade() >= PASS_THRESHOLD));

 
 
 
 
