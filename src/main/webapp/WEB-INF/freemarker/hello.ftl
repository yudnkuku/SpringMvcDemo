<html content="text/html;charset=utf-8">
    <head>
        <title>Freemarker</title>
    </head>
    <body>
        <#assign price=42/>
        <#assign x = 2.567/>
        <#assign foo = true/>
        <#list ["a","b","c"] as tmp>
            ${tmp}
        </#list>
        <br>
        <#assign x = 5/>
        ${x/2}
        ${12%10}
        <br>
        <#if 2 lt 5>
            2<5
            <#else>2>5
        </#if>
        <br>
        <#if 5 gt 2>
            5>2
        </#if>
        <br>
        <#assign age=25>
        <#if (age>60)>old man
            <#else>young man
        </#if>
        <br>
        <#list ["jack","lily","peter"] as names>
            <#if names_has_next>${names_index}.${names}</#if>
        </#list>
    </body>
</html>