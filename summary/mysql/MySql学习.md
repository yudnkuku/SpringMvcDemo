# MySql学习

标签（空格分隔）： MySQL

---

**创建数据库**：`CREATE DATABASE dbname`
**删除数据库**：`DROP DATABASE dbname`
**选择数据库**：`USE dbname`

**数据类型**
大致可分为三类：数值、日期/时间和字符串(字符)类型
**数值类型**：
|类型|大小|范围(有符号)|范围(无符号)|描述|
|--|--|--|--|--|
|TINYINT|1byte|(-128,127)|(0,255)|小整数值|
|SMALLINT|2bytes|(-32 768，32 767)|(0,65535)||
|MEDIUMINT|3bytes||||
|INT或INTEGER|4bytes||||
|BIGINT|8bytes||||
|FLOAT|4bytes|||单精度浮点数值|
|DOUBLE|8bytes|||双精度浮点数值|
|DECIMAL|对DECIMAL(M,D),如果M>D，为M+2否则为D+2|||小数值|

**日期类型**：
|类型|大小(byte)|范围|格式|描述|
|--|--|--|--|--|
|DATE|3||YYYY-MM-DD|日期值|
|TIME|3||HH:MM:SS|时间值或持续时间|
|YEAR|1||YYYY|年|
|DATETIME|8||YYYY-MM-DD HH:MM:SS|混合日期和时间值|
|TIMESTAMP|4||YYYYMMDD HHMMDD|混合日期和时间值，时间戳|

**字符串类型**：
|类型|大小(字节)|描述|
|--|--|--|
|CHAR|0-255|定长字符串|
|VARCHAR|0-65535|变长字符串|
|TINYBLOB|0-255|不超过255字符的二进制字符串|
|TINYTEXT|0-255|短文本字符串|
|BLOB|0-65535|二进制形式的长文本数据|
|TEXT|0-65535|长文本数据|
|MEDIUMBLOB|0-16777215|二进制形式的中等长度文本数据|
|MEDIUMTEXT|0-16777215|中等长度文本数据|
|LONGBLOB|0-4294967295|二进制形式的极大文本数据|
|LONGTEXT|0-4294967295|极大文本数据|

**创建数据表**：`CREATE TABLE table_name (column_name column_type);`
例如在`RUNOOB`数据库中创建数据表`runoob_tb1`

    CREATE TABLE IF NOT EXITS `runoob_tb1`(
        `runoob_id` INT UNSIGNED AUTO_INCREMENT,
        `runoob_title` VARCHAR(100) NOT NULL,
        `runoob_author` VARCHAR(40) NOT NULL,
        `submission_date` DATE,
        PRIMARY KEY(`runoob_id`)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8;

**删除数据表**：`DROP TABLE table_name;`
**删除表内数据**：`DELETE FROM table_name WHERE condition;`
**清除表内数据，保存表结构**：`TRUNCATE TABLE table_name;`

**插入数据**：`INSERT INTO table_name (field1, field2,...fieldN) VALUES
            (value1, value2,...valueN);`
**插入多条数据**：`INSERT INTO table_name (field1, field2,...fieldN) VALUES (valueA1, valueA2,...valueAN),(valueB1, valueB2,...valueBN),(valueC1,valueC2,...valueCN);`

**查询数据**：`SELECT column_name,column_name FROM table_name [WHERE condition] [LIMIT M] [OFFSET M];`
`LIMIT`属性类指定返回的记录条数，`OFFSET`属性指定开始查询的偏移量

**`WHERE`子句**：`SELECT * FORM table_name WHERE condition1 [AND\OR] condition2;`

**`UPDATE`查询**：`UPDATE table_name SET field1=value1, field2=value2 WHERE condition;`

**`DELETE`语句**：`DELETE FROM table_name WHERE condition;`

**`LIKE`子句**：`LIKE condition`
`LIKE`通常和`%`一起使用，匹配任意字符，如:

    SELECT * FORM runoob_tb1 WHERE runoob_author LIKE `%com`;

会选择`RUNOOB.com`和`RUNOOB1.com`

**`UNION`操作符**：用于连接两个以上的`SELECT`语句的结果到一个结果集合中，多个`SELECT`语句会删除重复的数据。

    SELECT expression1, expression2,...expression_n FROM table_name1
    WHERE condition 
    UNION [ALL|DISTINCT]
    SELECT expression1, expression2,...expression_n FROM table_name2
    WHERE condition;

`ALL`返回所有数据，包含重复数据；`DISTINCT`删除重复数据，`UNION`操作默认情况下已经删除了重复数据。

**排序**：

    SELECT field1, field2, ...filedN FROM table_name 
    ORDER BY field1[fieldN] [ASC|DESC];

如：
`SELECT * FORM runoob_tb1 ORDER BY submission_date ASC`(从表`runoob_tb1`中选择数据并按`submission_date`升序排序)

**GROUP BY**:根据一个或多个列对结果集进行分组，在分组的列上我们可以使用`COUNT`、`SUM`、`AVG`等函数

    SELECT column_name function(column_name) FROM table_name  
    WHERE colunm_name operator value GROUP BY column_name;

如：

    SELECT name, COUNT(*) FROM employee_tb1 GROUP BY name;
    
**使用WITH ROLLUP**：`WITH ROLLUP`可以再分组统计数据基础上再进行相同的统计(`SUM`,`AVG`,`COUNT`...)
例如：先分组再统计每个人登录的次数

    SELECT name, SUM(signing) as signin_count FROM employee_tb1 
    GROUP BY name WITH ROLLUP;
结果：

    +--------+--------------+
    | name   | singin_count |
    +--------+--------------+
    | 小丽 |            2 |
    | 小明 |            7 |
    | 小王 |            7 |
    | NULL   |           16 |
    +--------+--------------+
    4 rows in set (0.00 sec)
其中`NULL`表示所有人的登录次数总和，我们可以使用`COALESCE`函数来设置一个可以取代`NULL`的名称，该函数可以返回表达式列表中第一个非`NULL`值，如：

    SELECT coalesce(name, '总数'), SUM(singin) as singin_count FROM  employee_tbl GROUP BY name;
当`name！=null`时，返回`总数`。最后结果：

    +--------------------------+--------------+
    | coalesce(name, '总数') | singin_count |
    +--------------------------+--------------+
    | 小丽                   |            2 |
    | 小明                   |            7 |
    | 小王                   |            7 |
    | 总数                   |           16 |
    +--------------------------+--------------+
    4 rows in set (0.01 sec)
    
**Join(连接)的使用**：`JOIN`用在多张表查询数据场景，也就是所谓的联合查询，大致可分为三类：

 - `INNER JOIN(内连接)`：获取两个表中字段匹配关系的记录
 - `LEFT JOIN(左连接)`：获取左表所有记录，即使右表没有对应匹配的记录
 - `RIGHT JOIN(右连接)`：获取右表所有记录，即使左表没有对应匹配的记录

假设有两张表`tcount_tb1`和`runoob_tb1`，两张表数据如下：

    mysql> use RUNOOB;
    Database changed
    mysql> SELECT * FROM tcount_tbl;
    +---------------+--------------+
    | runoob_author | runoob_count |
    +---------------+--------------+
    | 菜鸟教程  | 10           |
    | RUNOOB.COM    | 20           |
    | Google        | 22           |
    +---------------+--------------+
    3 rows in set (0.01 sec)
     
    mysql> SELECT * from runoob_tbl;
    +-----------+---------------+---------------+-----------------+
    | runoob_id | runoob_title  | runoob_author | submission_date |
    +-----------+---------------+---------------+-----------------+
    | 1         | 学习 PHP    | 菜鸟教程  | 2017-04-12      |
    | 2         | 学习 MySQL  | 菜鸟教程  | 2017-04-12      |
    | 3         | 学习 Java   | RUNOOB.COM    | 2015-05-01      |
    | 4         | 学习 Python | RUNOOB.COM    | 2016-03-06      |
    | 5         | 学习 C      | FK            | 2017-04-05      |
    +-----------+---------------+---------------+-----------------+
    5 rows in set (0.01 sec)
**内连接查询语句**：

    SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM runoob_tbl a INNER JOIN tcount_tbl b ON a.runoob_author = b.runoob_author;
查询结果：

    +-------------+-----------------+----------------+
    | a.runoob_id | a.runoob_author | b.runoob_count |
    +-------------+-----------------+----------------+
    | 1           | 菜鸟教程    | 10             |
    | 2           | 菜鸟教程    | 10             |
    | 3           | RUNOOB.COM      | 20             |
    | 4           | RUNOOB.COM      | 20             |
    +-------------+-----------------+----------------+
    4 rows in set (0.00 sec)
    
**左连接查询语句**：

    SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM runoob_tbl a LEFT JOIN tcount_tbl b ON a.runoob_author = b.runoob_author;
查询结果：

    +-------------+-----------------+----------------+
    | a.runoob_id | a.runoob_author | b.runoob_count |
    +-------------+-----------------+----------------+
    | 1           | 菜鸟教程    | 10             |
    | 2           | 菜鸟教程    | 10             |
    | 3           | RUNOOB.COM      | 20             |
    | 4           | RUNOOB.COM      | 20             |
    | 5           | FK              | NULL           |
    +-------------+-----------------+----------------+
    5 rows in set (0.01 sec)
    
**右连接查询语句**：

    SELECT a.runoob_id, a.runoob_author, b.runoob.count FROM runoob_tb1 a RIGHT JOIN tcount_tb1 b ON a.runoob_author = b.runoob.author;
查询结果：

    +-------------+-----------------+----------------+
    | a.runoob_id | a.runoob_author | b.runoob_count |
    +-------------+-----------------+----------------+
    | 1           | 菜鸟教程    | 10             |
    | 2           | 菜鸟教程    | 10             |
    | 3           | RUNOOB.COM      | 20             |
    | 4           | RUNOOB.COM      | 20             |
    | NULL        | NULL            | 22             |
    +-------------+-----------------+----------------+
    5 rows in set (0.01 sec)
    
**NULL值处理**：关于`NULL`的条件运算比较特殊，不能使用`=NULL`或者`!=NULL`在列中查找`NULL`值，在`MySql`中，`NULL`值和任何其他值的比较（即使是`NULL`）永远返回`false`。

 - `IS NULL`：当列值是`NULL`时，返回`true`
 - `IS NOT NULL`：当列值不是`NULL`时，返回`true`
 - `<=>`：当比较的两个值都是`NULL`时，返回`true`

实例：

    SELECT * FROM runoob_tb1 WHERE runoob_count IS NULL;
    SELECT * FROM runoob_tb1 WHERE runoob_count IS NOT NULL;
    
**正则表达式匹配**：在`SQL`语句中使用正则表达式匹配查询条件，举几个例子。

    SELECT name FROM person_tb1 WHERE name REGEXP '^st';(查找name字段中以'st'开头的所有数据)
    SELECT name FROM person_tb1 WHERE name REGEXP 'st$';(查找name字段中以'st'结尾的所有数据)

**事务**：`MySql`事务主要用于处理操作量大，复杂度高的数据

 - 在`MySql`中只有使用了`Innodb`数据库引擎的数据库或表才支持事务
 - 事务处理可以用来维护数据库的完整性，保证成批的`SQL`语句要么全部执行，要么全部不执行
 - 事务用来管理`insert`、`update`、`delete`语句

一般来说，事务必须满足4个条件(ACID):原子性、一致性、隔离性、持久性

 - 原子性：一个事务中的所有操作，要么全部完成，要么全部不完成，不会结束在中间某个环节，如果发生错误，会被回滚到(rollback)事务最开始状态。
 - 一致性：在事务开始之前和事务结束之后，数据库的**粗体文本**完整性没有被破坏
 - 隔离性：数据库允许多个并发事务同时对数据进行读写和修改的能力，隔离性可以防止多个事务并发执行时由于交叉执行而导致的数据不一致
 - 持久性：事务处理结束后，对数据的修改就是永久的

在 `MySQL` 命令行的默认设置下，事务都是**自动提交**的，即执行 `SQL` 语句后就会马上执行 `COMMIT` 操作。因此要显式地开启一个事务务须使用命令 `BEGIN` 或 `START TRANSACTION`，或者执行命令 `SET AUTOCOMMIT=0`，用来禁止使用当前会话的自动提交。
事务控制语句：

 - `BEGIN`或`START TRANSACTION`：显式开启事务
 - `COMMIT`：提交事务
 - `ROLLBACK`：结束用户的事务，并撤销正在进行的所有未提交的修改
 - `SAVEPOINT identifier`：在事务中创建一个保存点，可以有多个
 - `RELEASE SAVEPOINT identifier`：删除事务保存点
 - `ROLLBACK TO identifier`：把事务回滚到保存点
 - `SET TRANSACTION`：用来设置事务的隔离级别。`InnoDB`提供的事务隔离级别有`READ UNCOMITTED`、`READ COMITTED`、`REPEATABLE READ`和`SERIALIZABLE`

事务处理主要有两种方法：
1、用`BEGIN`、`ROLLBACK`、`COMMIT`实现
2、直接用`SET`来改变提交模式

 - `SET AUTOCOMMIT=0`禁止自动提交
 - `SET AUTOCOMMIT=1`开启自动提交

**ALTER命令**：修改数据表名或者修改数据表字段

    ALTER TABLE table_name DROP field_name；
    ALTER TABLE table_name ADD field_name field_type;(插入到最后一列)
    ALTER TABLE table_name ADD field_name field_type [FIRST|AFTER] c;(用关键字FIRST和AFTER指定插入位置)
    ALTER TABLE table MODIFY c CHAR(10);(将字段c的类型修改成CHAR(10))
    ALTER TABLE table CHANGE i j BIGINT;(CHANGE关键字和MODIFY关键字不同，在CHANGE关键字后要紧跟修改的字段名，然后指定新的字段名和类型)
    ALTER TABLE table MODIFY i INT NOT NULL DEFAULT 100;(指定字段i为非空且设置默认值)
    ALTER TABLE table ALTER i SET DEFAULT 100;(设置字段默认值)
    ALTER TABLE table ALTER i DROP DEFAULT;(删除默认值)
    ALTER TABLE table MODIFY i INT [AFTER J | FIRST](只能用MODIFY修改字段相对位置)
    
## 索引 ##


## 临时表 ##
临时表在某些时候保存一些临时数据是非常有用的，临时表只在当前连接才可见，当关闭连接时，`MySql`会自动删除表并释放空间。

    CREATE TEMPORARY TABLE table (field1 type1,field2 type2,...fieldN typeN);(创建临时表)
    DROP TABLE table;(删除临时表)
    
## 复制表 ##
复制表结构和表数据

    CREATE TABLE targetTable SELECT * FROM sourceTable;(复制表结构和数据)
    CREATE TABLE targetTable LIKE sourceTable;(复制表结构)
    INSERT INTO targetTable SELECT * FROM sourceTable;(复制表数据)
    CREATE TABLE cloneTable AS (SELECT id, username AS uname, password AS pass FROM table);(复制某些字段并改名)
    CREATE TABLE cloneTable (id INTEGER NOT NULL PRIMARY KEY) AS (SELECT * FROM table);(在复制表的同时创建新字段)

如果要完全复制`MySQL`的数据表，包括表的结构、索引、主键和默认值等,需要重新手动创建一样的表结构，并插入数据。

    SHOW CREATE TABLE sourceTable \G;(格式化输出创建表语句，据此重新创建克隆表)
    INSERT INTO targetTable (f1,f2,...fn) SELECT f1,f2,...fn FROM sourceTable;(插入待克隆数据)
    
## 元数据 ##
获取服务器元数据
|命令|描述|
|--|--|
|SELECT VERSION()|服务器版本信息|
|SELECT DATABASE()|当前数据库名|
|SELECT USER()|当前用户名|
|SHOW STATUS|服务器状态|
|SHOW VARIABLES|服务器配置变量|

## 序列使用 ##
自增主键：`AUTO_INCREMENT`

    ALTER TABLE table AUTO_INCREMENT = 100;(修改自增步长)
    
## 处理重复数据 ##
使用`PRIMARY KEY`或者`UNIQUE`来保证数据的唯一性

**统计重复数据**

    SELECT COUNT(*) AS repetitions, last_name, first_name FROM table GROUP BY last_name, first_name HAVING repetitions > 1;
    
**过滤重复数据**
用`DISTINCT`关键字过滤重复数据或者使用`GROUP BY`读取数据中不重复的数据

    SELECT DISTINCT last_name, first_name FROM table;
    SELECT last_name, first_name FROM table GROUP BY last_name, first_name;

**删除重复数据**

    方案1：
    CREATE TABLE tmp SELECT last_name, first_name, sex FROM table GROUP BY last_name, first_name, sex;(去除重复数据并创建新表)
    DROP TABLE table;
    ALTER TABLE tmp RENAME TO table;(将新表重命名)
    方案2：
    ALTER IGNORE TABLE table
    ADD PRIMARY KEY (last_name, first_name);

**约束**
|约束|作用|样例|
|:-:|:-:|:-:|
|主键|表中行的唯一标识符，通过主键可直接定位到某一行，因此不能重复|`primary key` `PRIMARY KEY('id')`|
|唯一约束|规定一张表中某一列的值不能重复|`UNIQUE ('col')`|
|外键|一个表可以有多个外键，外键必须`REFERENCES`另一个表的主键，取值必须在它参考的列中值|`CONSTRAINT '约束名称' FOREIGN KEY ('fk_col') REFERENCES table_name ('col')`|
|非空约束|即限制该列取值不能为空|`NOT NULL`|

    

    CREATE TABLE employee
     (
       id      INT(10) PRIMARY KEY,
       name    CHAR(20),
       age     INT(10),
       salary  INT(10) NOT NULL,
       phone   INT(12) NOT NULL,
       in_dpt  CHAR(20) NOT NULL,
       UNIQUE  (phone),
       CONSTRAINT emp_fk FOREIGN KEY (in_dpt) REFERENCES department(dpt_name)    //dpt_name是表department的主键
      );

参考：[MySQL总结][1]


  [1]: https://www.cnblogs.com/programmer-tlh/p/5782418.html