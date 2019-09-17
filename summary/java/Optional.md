# Optional

标签（空格分隔）： Java8

---

`Optional`主要用来解决`NPE`空指针异常，我们从一个简单地示例开始，在`Java8`之前，任何访问对象方法或属性都可能导致`NullPointerException`异常：

    String isocode = user.getAddress().getCountry().getIsocode().toUpperCase();
    
在这个示例中，如果我们需要确保不触发异常，就得在访问每一个值之前对其进行明确的检查：

    if (user != null) {
    Address address = user.getAddress();
    if (address != null) {
        Country country = address.getCountry();
        if (country != null) {
            String isocode = country.getIsocode();
            if (isocode != null) {
                isocode = isocode.toUpperCase();
            }
        }
    }
}

这样代码变得冗长，难以维护，为了解决这些空指针检查的问题，我们可以使用`Optional`类，该类借鉴了`google guava`类库的`Optional`类。

## 创建Optional实例 ##
`Optional`类型的对象可能包含值，也可能为空：

    @Test(expected = NoSuchElementException.class)
    public void whenCreateEmptyOptional_thenNull() {
        Optional<User> emptyOpt = Optional.empty();
        emptyOpt.get();
    }
    
访问`emptyOpt`变量的值会导致`NoSuchElementException`异常。

也可以使用`of()`和`ofNullable()`方法创建包含之的`Optional`，两个方法的不同之处在于如果你把`null`值作为参数传递进去，`of()`方法会抛出`NullPointerException`异常：

    @Test(expected = NullPointerException.class)
    public void whenCreateOfEmptyOptional_thenNullPointerException() {
        Optional<User> opt = Optional.of(user);
    }
    
因此最佳实践应该是使用`ofNullable()`方法来创建`Optional`对象：

    Optional<User> opt = Optional.ofNullable(user);
    
## 访问Optional对象值 ##
方法一`get()`取值：

    @Test
    public void whenCreateOfNullableOptional_thenOk() {
        String name = "John";
        Optional<String> opt = Optional.ofNullable(name);
    
        assertEquals("John", opt.get());
    }
    
如果值为空，`get()`方法会抛出异常，为了避免异常首先需要使用`isPresent()`方法验证是否有值：

    @Test
    public void whenCheckIfPresent_thenOk() {
        User user = new User("john@gmail.com", "1234");
        Optional<User> opt = Optional.ofNullable(user);
        assertTrue(opt.isPresent());
    
        assertEquals(user.getEmail(), opt.get().getEmail());
    }
    
检查是否有值得另一个选择是`ifPresent()`方法，该方法除了执行检查，还接受一个`Conumser`参数，如果对象不是空的，就执行传入的`Lambda`表达式

    opt.ifPresent( u -> assertEquals(user.getEmail(), u.getEmail()));
    
在这个例子中，只有`user`不是空的时候才会执行断言

**返回默认值**

方式1：`orElse()`，如果有值则返回该值，否则返回传递给他的参数值

    @Test
    public void whenEmptyValue_thenReturnDefault() {
        User user = null;
        User user2 = new User("anna@gmail.com", "1234");
        User result = Optional.ofNullable(user).orElse(user2);
    
        assertEquals(user2.getEmail(), result.getEmail());
    }
    
这里`user`对象是空的，所以返回了作为默认值的`user2`，如果对象的初始值不是`null`，那么默认值会被忽略

    @Test
    public void whenValueNotNull_thenIgnoreDefault() {
        User user = new User("john@gmail.com","1234");
        User user2 = new User("anna@gmail.com", "1234");
        User result = Optional.ofNullable(user).orElse(user2);
    
        assertEquals("john@gmail.com", result.getEmail());
    }
    
方式2：`orElseGet()`，该方法在有值得时候返回值，没有值的时候执行作为参数传入的`Supplier`函数式接口，并返回其执行结果：

    User result = Optional.ofNullable(user).orElseGet( () -> user2);
    
**orElse() & orElseGet()**

看两个示例：

    @Test
    public void givenEmptyValue_whenCompare_thenOk() {
        User user = null
        logger.debug("Using orElse");
        User result = Optional.ofNullable(user).orElse(createNewUser());
        logger.debug("Using orElseGet");
        User result2 = Optional.ofNullable(user).orElseGet(() -> createNewUser());
    }
    
    private User createNewUser() {
        logger.debug("Creating New User");
        return new User("extra@gmail.com", "1234");
    }
    
打印结果：

    Using orElse
    Creating New User
    Using orElseGet
    Creating New User
    
可以看出，当`user`等于`null`时，`orElse`和`orElseGet`方法都执行了传入的`createNewUser`方法，接着再看一个示例：

    @Test
    public void givenPresentValue_whenCompare_thenOk() {
        User user = new User("john@gmail.com", "1234");
        logger.info("Using orElse");
        User result = Optional.ofNullable(user).orElse(createNewUser());
        logger.info("Using orElseGet");
        User result2 = Optional.ofNullable(user).orElseGet(() -> createNewUser());
    }

打印结果：

    Using orElse
    Creating New User
    Using orElseGet
    
可以看到，如果两个`Optional`对象都包含非空值，两个方法都会返回对应的非空值，不过**`orElse`方法仍然会创建`User`对象，相反，`orElseGet()`方法不创建`User`对象**

## 转换值 ##
有多种方法可以转换`Optional`的值，我们从`map()`和`flatMap()`方法开始，先来看看使用`map()`方法的例子：

    @Test
    public void whenMap_thenOk() {
        User user = new User("anna@gmail.com", "1234");
        String email = Optional.ofNullable(user)
          .map(u -> u.getEmail()).orElse("default@gmail.com");
    
        assertEquals(email, user.getEmail());
    }
    
`map()`对值调用作为参数的函数，然后将返回的值包装在`Optional`中(这对理解`map()`和`flatMap()`的不同有帮助)。可以看看`map()`的源码：

    public<U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent())
            return empty();
        else {
            return Optional.ofNullable(mapper.apply(value));  //调用入参函数，并将结果包装在Optional中
        }
    }

相比之下，`flatMap()`也需要函数作为参数，并对值调用这个函数，然后直接返回结果，返回的结果类型也是`Optional`，由于`flatMap()`直接返回`Optional`类型，因此需要传入的入参函数返回`Optional`类型，我们直接改造`User`类的`get`方法，使其返回`Optional`：

    public class User {    
    private String position;

    public Optional<String> getPosition() {
        return Optional.ofNullable(position);
    }

    //...
}

那么我们可以将`getPosition()`方法作为`Function`传入`flatMap`中调用：

    @Test
    public void whenFlatMap_thenOk() {
        User user = new User("anna@gmail.com", "1234");
        user.setPosition("Developer");
        String position = Optional.ofNullable(user)
          .flatMap(u -> u.getPosition()).orElse("default");
    
        assertEquals(position, user.getPosition().get());
    }

小结：`map()`方法和`flatMap()`方法都会调用传入的`mapper`函数，返回值都是`Optional`对象，区别在于，`map()`方法在调用`mapper`函数之后会将结果包装成`Optional`对象，而`flatMap`方法要求`mapper`方法返回`Optional`对象，可以看`flatMap`的源码：

    
    //Function函数返回Optional<U>类型
    public<U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent())
            return empty();
        else {
            return Objects.requireNonNull(mapper.apply(value));
        }
    }
    
    //Function函数返回 ？ extends U 类型，需要最后将其包装成Optional类型
    public<U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent())
            return empty();
        else {
            return Optional.ofNullable(mapper.apply(value));  //包装成Optional对象
        }
    }
    
## 链式语法 ##
为了更充分的使用`Optional`，可以使用链接组合其大部分方法，因为它们都返回相同类似的对象，重写`User`类，使其`getter`方法返回`Optional`对象：

    public class User {
        private Address address;
    
        public Optional<Address> getAddress() {
            return Optional.ofNullable(address);
        }
    
        // ...
    }
    public class Address {
        private Country country;
    
        public Optional<Country> getCountry() {
            return Optional.ofNullable(country);
        }
    
        // ...
    }

现在可以删除`null`检查，替换为`Optional`的方法：

    @Test
    public void whenChaining_thenOk() {
        User user = new User("anna@gmail.com", "1234");
    
        String result = Optional.ofNullable(user)
          .flatMap(u -> u.getAddress())
          .flatMap(a -> a.getCountry())
          .map(c -> c.getIsocode())
          .orElse("default");
    
        assertEquals(result, "default");
    }

上面的代码可以通过方法引用进一步缩减：

    String result = Optional.ofNullable(user)
      .flatMap(User::getAddress)
      .flatMap(Address::getCountry)
      .map(Country::getIsocode)
      .orElse("default");

