package demo;

import demo.optional.User;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamTest {

    class Student {
        int no;
        String name;
        String sex;
        float height;

        public Student(int no, String name, String sex, float height) {
            this.no = no;
            this.name = name;
            this.sex = sex;
            this.height = height;
        }

        public int getNo() {
            return no;
        }

        public void setNo(int no) {
            this.no = no;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }
    }

    @Test
    public void test() {
        Student s1 = new Student(1, "A", "male", 180);
        Student s2 = new Student(2, "B", "female", 160);
        Student s3 = new Student(3, "C", "male", 170);
        Student s4 = new Student(4, "D", "male", 190);
        List<Student> list = new ArrayList<>();
        list.add(s1);
        list.add(s2);
        list.add(s3);
        list.add(s4);
        list.stream()
            .filter(s -> s.getSex().equals("male"))
            .forEach(s -> System.out.println(s.getName()));
    }

    /**  Stream iterate **/
    @Test
    public void test1() {
        Stream.iterate(1, item -> item+1)
            .limit(10)
            .forEach(System.out::println);
    }

    /** Intermediate **/
    @Test
    public void testConcat() {
        Stream.concat(Stream.of(4,5), Stream.of(1,2,3))
            .forEach(System.out::println);
    }

    @Test
    public void testDistinct() {
        Stream.of(1,2,3,2,1)
            .distinct()
            .forEach(System.out::println);
    }

    @Test
    public void testFilter() {
        Stream.of(1,2,3,4)
            .filter(item -> item > 3)
            .forEach(System.out::println);
    }

    @Test
    public void testMap() {
        System.out.println("test map");
        Stream.of("a", "b", "hello")
            .map(item -> item.toUpperCase())
            .forEach(System.out::println);
        System.out.println("test mapToDouble");
        Stream.of(1,2,3)
            .mapToDouble(item -> Double.valueOf(item))
            .forEach(System.out::println);
    }

    @Test
    public void testMapToInt() {
        Stream.of('a', 'b', 'c')
            .mapToInt(item -> Integer.valueOf(item))
            .forEach(System.out::println);
    }

    /**
     * flatMap方法和map方法类似，不同的是flatMap的转换对象是一个Stream
     */
    @Test
    public void testFlatMap() {
        Stream.of(1,2,3)
            .flatMap(item -> Stream.of(item*10))
            .forEach(System.out::println);
    }

    @Test
    public void testPeek() {
        Stream.of(1,2,3,4,5)
            .peek(integer -> System.out.println("accept:" + integer))
            .forEach(System.out::println);
    }

    @Test
    public void testSkip() {
        Stream.of(1,2,3,4,5)
            .skip(6)
            .forEach(System.out::println);
    }

    @Test
    public void testSorted() {
        Stream.of(2,4,5,3,1)
            .sorted((m, n) -> {
                if (m > n) {
                    return -1;
                } else if (m < n) {
                    return 1;
                } else {
                    return 0;
                }
            })
            .forEach(System.out::println);
    }

    @Test
    public void testSorted1() {
        Stream.of(1,2,3,4,5)
            .sorted()
            .forEach(System.out::println);

        System.out.println("======");
        Stream.of(5,4,3,2,1)
            .sorted()
            .forEach(System.out::println);
    }

    /** Terminal **/
    @Test
    public void testCount() {
        long count = Stream.of(1,2,3,4,5)
            .count();
        System.out.println("count: " + count);
    }

    @Test
    public void testMax() {
        Optional<Integer> max = Stream.of(1,2,3)
                                    .max((o1,o2) -> o2-o1);
        System.out.println("max: " + max.get());
    }

    @Test
    public void testMin() {
        Optional<Integer> min = Stream.of(1,2,3)
                                    .min((o1,o2) -> o1-o2);
        System.out.println("min: " + min.get());
    }

    @Test
    public void testCollect() {
        List<Integer> collectList = Stream.of(1,2,3)
                                        .map(integer -> integer*2)
                                        .collect(Collectors.toList());
        System.out.println(collectList);
    }

    /** reduce 聚合函数 **/
    @Test
    public void testReduce() {
        //没有提供初始值，返回值是Optional
        Optional<Integer> sum = Stream.of(1,2,3)
                                    .reduce((a,b) -> a+b);
        System.out.println(sum.orElseGet(() -> 0));
    }

    @Test
    public void testReduce1() {
        //提供初始值
        Integer sum = Stream.of(1,2,3)
                        .reduce(5, (a,b) -> a+b);
        System.out.println(sum);
    }

    @Test
    public void testReduce2() {
        System.out.println("给定个初始值，求和：");
        System.out.println(Stream.of(1,2,3).reduce(10, (sum,item) -> sum+item));
        System.out.println(Stream.of(1,2,3).reduce(10, Integer::sum));
        System.out.println("给定个初始值，求最小值min:");
        System.out.println(Stream.of(1,2,3).reduce(5, (min,item) -> Math.min(min, item)));
        System.out.println(Stream.of(1,2,3).reduce(5, Math::min));
        System.out.println("给定个初始值，求最大值max:");
        System.out.println(Stream.of(1,2,3).reduce(5, (max,item) -> Math.max(max, item)));
        System.out.println(Stream.of(1,2,3).reduce(5, Math::max));

        System.out.println("无初始值，求和：");
        System.out.println(Stream.of(1,2,3).reduce(Integer::sum).orElse(0));
        System.out.println("无初始值，求最小值min:");
        System.out.println(Stream.of(1,2,3).reduce(Integer::min).orElse(0));
        System.out.println("无初始值，求最大值max:");
        System.out.println(Stream.of(1,2,3).reduce(Math::max).orElse(0));
    }

    @Test
    public void testIterate() {
        Stream.iterate(10, item -> item-1)
            .limit(10)
            .forEach(System.out::println);
    }

    @Test
    public void testCollection() {
        List<User> users = new ArrayList<>();
        users.add(new User("wuhan", "China", "430073"));
        users.add(new User("shanghai", "China", "000010"));
        users.add(new User("anlu", "China", "430000"));
        System.out.println("======test Collectors.toCollection TreeSet");
        Set<String> cities = users.stream()
                                    .map(User::getAddress)
                                    .collect(Collectors.toCollection(TreeSet::new));
        System.out.println(cities);
        System.out.println("======test Collectors.joining");
        String joinAddr = users.stream()
                                .map(User::getAddress)
                                .collect(Collectors.joining(","));
        System.out.println(joinAddr);

        System.out.println("======test Collectors.summingDouble");
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("deacon", 26, 10000, "研发"));
        employees.add(new Employee("jack", 30, 15000,"研发"));
        employees.add(new Employee("lina", 27, 12000, "测试"));
        double salarySum = employees.stream()
                                    .collect(Collectors.summingDouble(Employee::getSalary));
        System.out.println(salarySum);

        System.out.println("======tesy Collectors.groupingBy");
        Map<String, List<Employee>> byDept
                = employees.stream()
                            .collect(Collectors.groupingBy(Employee::getDepartment));
        System.out.println(byDept);

        System.out.println("======test Collectors.groupingBy and summing");
        Map<String, Double> totalByDept
                = employees.stream()
                            .collect(Collectors.groupingBy(Employee::getDepartment,
                                    Collectors.summingDouble(Employee::getSalary)));
        System.out.println(totalByDept);

        System.out.println("======test Collectors.passingBy");
        Map<Boolean, List<Employee>> highSalaryEmp
                = employees.stream()
                            .collect(Collectors.partitioningBy(employee -> employee.getSalary()>12000));
        System.out.println(highSalaryEmp);

    }

    /** test Short-circuiting **/
    @Test
    public void testAllMatch() {
        boolean allPositive = Stream.of(1,2,3,4,5)
                                .allMatch(n -> n>0);
        System.out.println("all positive: " + allPositive);
    }

    @Test
    public void testAnyMatch() {
        boolean anyMatch = Stream.of(1,2,3,4)
                                .anyMatch(n -> n>5);
        System.out.println("any match: " + anyMatch);
    }

    @Test
    public void testFindAny() {
        Optional<Integer> random = Stream.of(1,2,3)
                                        .findAny();
        System.out.println("findAny: " + random.orElse(10));
    }

    @Test
    public void testFindFirst() {
        Optional<Integer> first = Stream.of(2,1,3)
                                        .findFirst();
        System.out.println("first: " + first.orElse(10));
    }

    @Test
    public void testLimit() {
        Stream.of(1,2,3,4,5)
            .limit(2)
            .forEach(System.out::println);
    }

    @Test
    public void testLimit1() {
        Stream.generate(Math::random)
            .limit(10)
            .forEach(System.out::println);
    }

    class Employee {

        String name;
        int age;
        double salary;
        String department;

        public Employee() {
        }

        public Employee(String name, int age, double salary, String department) {
            this.name = name;
            this.age = age;
            this.salary = salary;
            this.department = department;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }
    }

}
