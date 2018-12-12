# Spring Form Tag

标签（空格分隔）： Spring form标签学习

---

在使用`Spring MVC`的时候可以使用到`Spring`封装的一系列表单标签，这些标签可以访问到`ModelMap`里面的内容，通常要在`JSP`文件开头加入以下代码：

    <@taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

## 1 form标签 ##
使用`Spring`的`form`标签主要有两个作用，第一会自动绑定来自`Model`的属性值到`form`对应的实体对象(将`command`对象放到`PageContext`上下文中，这样所有的内置标签都能访问该对象)，默认是`command`属性，第二是它支持在提交表单时使用除`GET`和`POST`之外的其他方式进行提交
## 1.1 绑定表单对象 ##
先看个实例：

    <form:form action="formTag/form.do" method="post">  
        <table>  
            <tr>  
                <td>Name:</td><td><form:input path="name"/></td>  
            </tr>  
            <tr>  
                <td>Age:</td><td><form:input path="age"/></td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>  

这个时候如果`Model`中存在一个名称为`command`的属性值，那么会自动绑定到`form`对象，通过`path`属性来访问该对象的属性值并渲染，假如`command`的属性值是一个`javabean`，且其`name`和`age`属性值分别为`zhangsan`和`36`，就会渲染成：

    <form id="command" action="formTag/form.do" method="post">  
        <table>  
            <tr>  
                <td>Name:</td><td><input id="name" name="name" type="text" value="ZhangSan"/></td>  
            </tr>  
            <tr>  
                <td>Age:</td><td><input id="age" name="age" type="text" value="36"/></td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form>  

我们可以使用`commandName`或者`modelAttribute`属性来指定要绑定的`Model`中的属性名称，替代默认的`command`

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>Name:</td><td><form:input path="name"/></td>  
            </tr>  
            <tr>  
                <td>Age:</td><td><form:input path="age"/></td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>  

## 2 input标签 ##
`input`标签会被渲染为一个普通的`input text`标签，但是其可以绑定模型数据，不需要绑定数据时使用普通的`input`标签即可

    <form:form action="formTag/form.do" method="head" modelAttribute="user" methodParam="requestMethod">  
        <table>  
            <tr>  
                <td>Name:</td><td><form:input path="name"/></td>  
            </tr>  
            <tr>  
                <td>Age:</td><td><form:input path="age"/></td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form> 
    
## 3 checkbox标签 ##
`checkbox`复选框的状态和绑定数据有关
## 3.1 绑定boolean数据 ##
当绑定数据为`boolean`类型时，`checkbox`的状态和该数据值一样，即`true`选中，`false`不选中

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>Male:</td><td><form:checkbox path="male"/></td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form> 
当`${user.male}`的值为`true`时选中

## 3.2 绑定列表数据 ##
这里的列表数据包括数组、`List`和`Set`，下面以`List`为例子介绍如何绑定数据，假设有个一类`User`，其中有个类型为`List`的属性`roles`

    public class User {  
       
        private List<String> roles;  
       
        public List<String> getRoles() {  
           return roles;  
        }  
       
        public void setRoles(List<String> roles) {  
           this.roles = roles;  
        }  
    }  
    
那么当我们需要展现该`User`中是否存在某个`Role`时：

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>Roles:</td>  
                <td>  
                   <form:checkbox path="roles" value="role1"/>Role1<br/>  
                   <form:checkbox path="roles" value="role2"/>Role2<br/>  
                   <form:checkbox path="roles" value="role3"/>Role3  
                </td>  
            </tr>  
        </table>  
    </form:form>
**小结一下**：还是通过`path`指定模型中`List`属性名称，通过判断`value`属性值是否存在于`List`中来确定`checkbox`状态

## 3.3 绑定Object数据 ##
通过判断`value`属性值和对象`toString()`方法返回的字符串是否相等来确定`checkbox`状态
模型属性对象：

public class User {  
   
    private Blog blog;  
     
    public Blog getBlog() {  
       return blog;  
    }  
   
    public void setBlog(Blog blog) {  
       this.blog = blog;  
    }  
}  
`Blog`代码：

    public class Blog {
        public String toString() {
            return "HelloWorld";
        }
    }

`JSP`代码：

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>HelloWorld:</td>  
                <td>  
                   <form:checkbox path="blog" value="HelloWorld"/>  //比较value属性值和blog.toString()是否相等 
                </td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>  

## 4 checkboxes ##
## 4.1 获取list属性 ##
要点：还是通过`path`属性获取模型属性，通过`items`属性获取模型属性进行展示，然后判断`items`中是否包含`path`属性对应的值来确定`checkboxes`状态

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
               <td>Roles:</td>  
                <td>  
                   <form:checkboxes path="roles" items="${roleList}"/>  //items属性直接获取模型属性(不是在user下获取属性值)，用于展示，如果包含`roles`属性，则勾选  
                </td>  
            </tr>  
            <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>  

此时`ModelMap`里应该包含`user.roles`和`roleList`

## 4.2 获取map属性 ##
上述数组、`List`和`Set`作为数据源时，展现的`Label`和实际上送的`value`是一样的，如果需要两者不一样，则需要修改数据源为`Map`，此时`Map`中的`value`用于展示，而`key`用来判断`checkboxes`的状态

    控制器代码：
    
    @RequestMapping(value="form", method=RequestMethod.GET)  
    public String formTag(Map<String, Object> map) {  
       User user = new User();  
       List<String> roles = new ArrayList<String>();  
       roles.add("role1");  
       roles.add("role3");  
       user.setRoles(roles);  
       Map<String, String> roleMap = new HashMap<String, String>();  
       roleMap.put("role1", "角色1");  
       roleMap.put("role2", "角色2");  
       roleMap.put("role3", "角色3");  
       map.put("user", user);  
       map.put("roleMap", roleMap);  
       return "formTag/form";  
    }  

    视图代码：
    <form:form action="formTag/form.do" method="post" commandName="user">  
    <table>  
        <tr>  
            <td>Roles:</td>  
            <td>  
               <form:checkboxes path="roles" items="${roleMap}"/>    
            </td>  
        </tr>  
        <tr>  
            <td colspan="2"><input type="submit" value="提交"/></td>  
        </tr>  
    </table>  
</form:form>

## 5 radiabutton/radiobuttons标签 ##
同样通过对比`value`属性和`path`属性值是否一样来判断组件状态

## 6 select+option标签 ##
`select`通常和`option`标签一起使用，先看个例子

    控制器代码：
        @RequestMapping(value="form", method=RequestMethod.GET)  
    public String formTag(Map<String, Object> map) {  
       User user = new User();  
       user.setFavoriteBall(4);//设置我最喜爱的球类运动是4羽毛球  
       Map<Integer, String> ballMap = new HashMap<Integer, String>();  
       ballMap.put(1, "篮球");  
       ballMap.put(2, "足球");  
       ballMap.put(3, "乒乓球");  
       ballMap.put(4, "羽毛球");  
       ballMap.put(5, "排球");  
       map.put("user", user);  
       map.put("ballMap", ballMap);  
       return "formTag/form";  
    }  
    
    JSP代码：
    
    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>最喜欢的运动:</td>  
                <td>  
                   <form:select path="favoriteBall">  
                       <option>请选择</option>  
                       <form:option value="1">篮球</form:option>  
                       <option value="4">羽毛球</option>  
                   </form:select>  
                </td>  
            </tr>  
           <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>  
其渲染结果：
下拉栏中三项：请选择、篮球、羽毛球
很显然`form:select`不会匹配`option`标签，只会匹配`form:option`标签，当我们在其中加入一个标签如下：

    <form:form action="formTag/form.do" method="post" commandName="user">  
    <table>  
        <tr>  
            <td>最喜欢的运动:</td>  
            <td>  
               <form:select path="favoriteBall">  
                   <option>请选择</option>  
                   <form:option value="1">篮球</form:option>  
                   <option value="4">羽毛球-A</option>  
                   <form:option value="4">羽毛球-B</form:option>  
               </form:select>  
            </td>  
        </tr>  
       <tr>  
            <td colspan="2"><input type="submit" value="提交"/></td>  
        </tr>  
    </table>  
</form:form> 
这就会渲染为：
四个下拉选项：请选择、篮球、羽毛球A、羽毛球B
很显然会匹配`form:option`标签
还可以通过在`form:select`标签中指定数据源替换子标签里的内容:

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>最喜欢的运动:</td>  
                <td>  
                   <form:select path="favoriteBall" items="${ballMap}">  
                       <option>请选择</option>  
                       <form:option value="1">篮球</form:option>  
                       <option value="4">羽毛球</option>  
                   </form:select>  
                </td>  
            </tr>  
           <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>
`form:select`标签下的`option`或者`form:option`标签统统无效不会显示，而只会显示`form:select`标签`item`属性值指定`map`的内容

## 7 options标签 ##
需要指定`items`属性，生成一系列`option`标签显示

    <form:form action="formTag/form.do" method="post" commandName="user">  
        <table>  
            <tr>  
                <td>最喜欢的运动:</td>  
                <td>  
                   <form:select path="favoriteBall">  
                       <option>请选择</option>  
                       <form:options items="${ballMap}"/>  
                   </form:select>  
                </td>  
            </tr>  
           <tr>  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form> 

## 8 errors标签 ##
`Spring MVC errors`标签就是对应`Errors`对象的，它的作用就是展现`Errors`对象中包含的错误信息。通过`path`来展现两种类型的错误信息：

 - `path="*"`：展示所有错误
 - 单独展现某个域的错误信息，这个时候`path`应该取值对应域名称
 - 如果`path`省略，则只会显示对象错误信息

        <form:form action="formTag/form.do" method="post" commandName="user">  
        <table border="1px" bordercolor="blue">  
            <tr align="center">  
                <td width="100">姓名:</td>  
                <td width="150"><form:input path="name"/></td>  
            </tr>  
            <tr align="center">  
                <td>用户名:</td>  
                <td><form:input path="username"/></td>  
            </tr>  
            <tr>  
                <td>所有错误信息:</td>  
                <td><form:errors path="*"/></td>  
            </tr>  
            <tr>  
                <td>Name的错误信息:</td>  
                <td><form:errors path="name"/></td>  
            </tr>  
            <tr align="center">  
                <td colspan="2"><input type="submit" value="提交"/></td>  
            </tr>  
        </table>  
    </form:form>  