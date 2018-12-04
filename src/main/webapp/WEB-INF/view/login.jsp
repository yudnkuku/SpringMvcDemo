<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!Doctype>
<html>
    <head>
        <title>Login</title>
    </head>

<body>
    <form:form action="user" method="post" modelAttribute="user">
        <p>用户名：<form:input path="username" type="text"/></p>
        <form:errors path="username" cssStyle="color:red"/>
        <p>密码：<form:input path="password"/></p>
        <form:errors path="password" cssStyle="color:red"/>
        <p>E-mail：<form:input path="email"/> </p>
        <form:errors path="email" cssStyle="color:red"/>
        <p>出生日期：<form:input path="birthday"/></p>
        <form:errors path="birthday" cssStyle="color:red"/>
        <button type="submit" id="submit" value="submit">Submit</button>
    </form:form>
</body>
</html>