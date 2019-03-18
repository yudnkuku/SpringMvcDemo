# Git笔记

标签（空格分隔）： Git

---

学习廖雪峰的`Git`教程笔记，[廖雪峰git教程][1]
## 创建版本库 ##
主要命令：`git init`
该命令默认在`c:\Users\Administrator\`下创建`.git`目录，如在指定目录下创建版本库，需要修改为：

    cd path //进入指定目录
    git init repository //创建版本库，repository为版本库名称

接着在版本仓库下新建文件，例如`readme.txt`，使用命令：`git add readme.txt`添加到仓库，使用多个`git add`添加多个文件
最后用`git commit -m "xxx"`命令提交修改,`xxx`为提交内容描述
**总结**

 - `git init repository`：初始化版本库
 - `git add filename`:添加文件到版本库
 - `git commit -m "xxx"`：提交文件
 
## 查看状态 ##
主要命令：`git status`，查看版本库的当前状态
例如我修改了`readme.txt`，可以用`git status`查看版本库状态
`git diff`查看文件修改

## 版本回退 ##
主要命令：`git log --pretty=oneline`查看提交日志，后面的`--pretty=oneline`表示在一行显示
`git reset --hard HEAD^`返回上一版本
`git reset --hard HEAD~N`回退N个版本
`git reset --hard id`回退至某版本号
`git reflog`查看命令历史

## 工作区和暂存区 ##
工作区指的是`.git`上级目录
而目录`.git`可以被称作`GIT`的版本库，而版本库存放了很多东西，其中最重要的就是称为`stage`(或者`index`)的暂存区，`GIT`还为我们创建了一个分支`master`，并且有一个`HEAD`指针指向`master`
我们在提交修改的时候是分两步进行：

 - `git add`：将修改添加到暂存区(`stage`)
 - `git commit`:将暂存区所有文件提交到`master`分支，如果之前没有执行`git add`，命令行会报错`Changes not staged for commit`，执行完`git commit`后如果成功，命令行会显示`working tree clean`表示工作区是干净的，暂存区没有任何内容了
 - `git commit -a -m "msg"`：这是一个组合操作，对于修改或者删除的文件直接提交到当前分支，而新增的文件不会受到影响


## 撤销修改 ##
主要命令：`git checkout file`将工作区的修改撤销
`git reset HEAD file`将暂存区的修改`unstage`
`git reset --hard HEAD~N`将`master`分支的版本回退`N`次

## 删除文件 ##
主要命令：`rm file`删除工作区文件，撤销工作区文件删除使用`git checkout file`，提交删除还是先`git add file`再`git commit -m "delete xxx"`
`git rm file`删除版本库文件，接着提交删除修改`git commit -m ""delete file`,撤销修改使用之前的命令`git reset --hard id`回退到某版本
假如使用`rm file`删除文件管理器中的文件，如果是误删可以使用`git checkout file`与版本库同步；如果确实要删除直接`git rm file`再提交`git commit -m "delete xxx"`，如果直接使用命令`git rm file`会连同工作区的文件也删掉

## 添加远程库 ##
主要命令：`git remote add origin git@github.com:path/repository`将本地`git`仓库`master`关联到远程仓库，`origin`是远程仓库的名字，默认值
`git push -u origin master`将本地仓库修改推送到远程仓库
第一次提交需要加`-u`，之后提交直接`git push origin master`
第一次`push`时，会出现报错，提示没有权限，调用命令`ssh-keygen -t rsa -C "email"`，将在目录中生成一些文件，将`id_rsa.pub`里的内容复制到远程仓库新建的`SSH key`中即可`push`

## 克隆远程仓库 ##
主要命令：`git clone git@github.com:yudnkuku/CloneTest.git`将远程仓库`clone`到本地，在当前目录新建`git`仓库


## 创建与合并分支 ##
主要命令：

 - `git branch dev`创建`dev`分支
 - `git branch`查看所有分支，当前分支用`*`标明
 - `git branch -d dev`删除`dev`分支
 - `git checkout dev`切换到`dev`分支
 - `git checkout -b dev`创建并切换到分支，上面命令的结合
 - `git merge dev`合并`dev`分支,更新分支`master`的内容

## 解决冲突 ##
当`Git`无法自动合并分支时，必须先解决冲突，解决冲突后再提交，合并完成
解决冲突就是把`Git`合并失败的文件手动编辑为我们需要的内容，再提交
使用命令`git log --graph --pretty=oneline`可以查看分支合并图

## 分支管理策略 ##
通常，合并分支时，`Git`会用`Fast Forward`模式，但这种模式下删除分支之后会丢掉分支信息，如果强制禁用`ff`模式，`Git`会在`merge`时生成一个新的`commit`，从分支历史上会看到分支信息
主要命令：`git merge --no-ff -m "xxx" dev`使用`no ff`模式合并`dev`分支，因为会生成一个新的`commit`，使用`-m`标明提交信息

## Bug分支 ##
修复`bug`时，我们会通过创建新的`bug`分支进行修复，然后合并，最后删除分支
当手头工作没有完成时，先把工作现场`git stash`一下，然后去修复`bug`，修复后再`git stash pop`回到工作现场
主要命令：
`stash`中类似栈的结构
 - `git stash`保存工作现场，保存后工作区是干净的(`git status`)，可在当前工作区拉分支修复`bug`
 - `git stash list`当前工作现场列表
 - `git stash apply stash@{0}`恢复工作现场
 - `git stash drop stash@{0}`删除工作现场
 - `git stash pop`恢复并删除当前工作现场

## Feature分支 ##
每添加一个新功能，最好新建一个`feature`分支，在该分支上开发，完成后，合并，最后删除`feature`分支
流程如下：

 - `git checkout -b feature`新建并切换到`feature`分支
 - `git add file`/`git commit -m "xxx"`修改并提交
 - `git chekckout dev`切换到`dev`分支准备合并
 - `git merge --no-ff -m "xxx" feature`合并`feature`分支到`dev`
 - 此时如果不需要新功能，使用`git branch -d feature`删除分支，`git branch -D feature`强行删除分支
 

## 多人协作 ##
1、首先，可以试图用`git push origin <branch-name>`推送自己的修改
2、如果推送失败，是因为远程分支比你的本地更新，需要先用`git pull`试图合并
3、如果合并有冲突，解决冲突，并在本地提交
4、没有冲突或者解决掉冲突后，再用`git push origin <branch-name>`推送就能成功
如果`git pull`提示`no tracking information`，则说明本地分支和远程分支的链接关系没有建立，使用命令`git branch --set-upstream-to <branch-name> origin/<branch-name>`
`git remote -v`查看远程仓库信息

## Rebase ##
`git rebase`操作将本地未`push`的分叉历史整理成直线

**Git流程图**

![Git流程图][2]

## Git分支模型 ##

`Git`分支介绍：

1、`master`：主干分支，发布到生产的代码
`origin/master`就是主要分支，其`HEAD`指向的源码反映了生产准备状态，`origin/develop`中`HEAD`指向的源码反映了最近为下一个发布版本提交的更改，这个分支也叫做集成分支，当`develop`分支的代码到达一个稳定点，并准备好发布时，其所有的改变都必须`merge`到`master`，并`tag`上发布版本编号

2、`develop`:开发分支，预发布到生产的代码

3、`release`:新版本分支，新版本要发布到生产的代码
`release`分支可以从`develop`分支拉取，然后合并到`develop`和`master`分支，`release`分支通常用来准备新的生产版本发布

4、`feature`:新需求开发分支
`feature`分支一般从`develop`分支拉取，必须`merge`回`develop`。

`feature-branch`

![feature branch][3]

`feature`分支用来开发新需求，从`develop`分支拉取，最后`merge`回`develop`分支

    $ git checkout -b myfeature develop //创建feature分支并切换到feature分支
    $ git checkout develop //切换到develop分支
    $ git merge --no-ff -m "new feature" myfeature   //将feature分支merge回develop
    $ git branch -d myfeature   //删除feature分支
    $ git push origin master

`--no-ff`合并时会创建一个新的`commit`，尽管可能是`fast-forward`合并方式，这避免了丢失`feature`分支的历史信息，对比如下：

![两种merge方式的区别][4]

5、`hotfix`:紧急修复生产`bug`的代码

`hotfix-branch`

![hotfix-branch][5]

`Git`分支模型图：

![Git分支模型][6]

下面举一些可能在工作中面对的场景：
1、组长分配新需求下来，安排下周上线(假设是12.27号)，你看看当前有没有下周版本的分支？有的话很简单，`checkout`下周分支(`feature_app1.0.0_12.27`)来开发就行，没有的话需要从`develop`分支创建一个新的`feature`分支(`feature_app1.1.0_12.27`)，然后将对应的`pom.xml`版本号修改成`1.1.0-SNAPSHOT`，注意命名，比如这里我用`feature`做前缀，你也可以自己设定一个规则

2、开发完`feature_app1.1.0_12.27`需求，移交了测试，很遗憾出现了`n`个`bug`，这时依旧在`feature_app1.1.0_12.27`上修复`bug`

3、终于到了发版前一天，测试说`n`轮测试完了，没问题，拉上线版本，再做一次回归测试，这时，你就需要把`feature_app1.1.0_12.27`分支合并到`develop`分支，然后从`develop`分支中创建新的分支`release_app1.1.0_12.27`，然后修改对应的版本号为`1.1.0-RELEASE`

4、到了发版日早上了，测试组用了`release_app1.1.0_12.27`版本测试了一番，又发现了一个`bug`，别慌，只要不是生产`bug`都好解决，这时你只需要在`release_app1.1.0_12.27`上修复`bug`，切记不能在`feature_app1.1.0_12.27`上修改，`feature_app1.1.0_12.27`分支已经没有多大作用了，只用来查看代码提交记录

5、发版完成，这时别忘了把`release_app1.1.0_12.27`版本合并到`develop`和`master`分支上，还有一点很重要，把`develop`分支代码合并到1227以后的版本(如果已经有1227以后的版本的话)，注意这个步骤合并代码要谨慎，如果有别人的代码合并冲突比较大，需要找那个开发的同事一起合并代码、

6、告别了旧需求，迎来新需求就按照上面的流程走

7、第二天，生产环境下一直报`NPE`异常，在`master`分支上拉取一个`hotfix_app1.1.1_1228`分支，修复`NPE`，打包上线，验证没问题了将`hotfix_app1.1.1_1228`分支合并到`develop`和`master`分支，并把`develop`分支合并到1227以后的版本

## 命令详解 ##
1、`git fetch`:从其他仓库下载对象和引用，当没有指定远程仓库时，会默认使用`origin`远程仓库，除非当前分支有上游分支，`fetch`的引用和它们指向的对象名称都会被写入`.git/FETCH_HEAD`文件中，这些信息也会被`git`的其他命令使用，例如`git pull`

2、`git pull`:从其他仓库`fetch`并和本地分支合并，`git pull`命令等价于`git fetch`和`git merge FETCH_HEAD`，也就是说`git pull`实际上先调用`git fetch`(可能带有指定参数)，然后调用`git merge`将`fetch`到的分支合并到当前分支，如果使用`--rebase`选项，那么会使用`git rebase`替代`git merge`

3、`git branch`:当前分支会以星号标注

4、`git merge`:将两个或者多个开发历史合并，这个命令也被`git pull`调用，也可以手动调用合并分支

       A---B---C topic
      /
     D---E---F---G master

调用`git merge topic`后，会生成一个新的`commit`：

       A---B---C topic
      /         \
     D---E---F---G---H master

5、`git reset`:将当前`HEAD`重置到指定的提交状态
通常该命令会有如下形式：

    git reset [<mode>] [<commit>]
    
这种形式会将当前分支`HEAD`指向`<commit>`指定的版本，可能会更新引用(将其重置为`<commit>`所指的树)，并且该工作树(`working tree`)依赖于`<mode>`，如果`<mode>`省略，more是`--mixed`，`<mode>`的可用选项如下：

 - `--soft`:不会修改`index`区(也就是暂存区)或者工作区，但会将分支的`HEAD`指向`<commit>`，这时`stage`区和本地分支仓库状态就不一致，那么用`git status`打出来就是`Changes to be committed`
总结一下：`--soft`会将当前分支库回退版本，但是不会回退暂存区，也不会清空工作区修改
 - `--mixed`:重置`index`但不会重置工作树(例如修改的文件会被保留但是不会被标记为需要`commit`)，这是默认的配置
总结一下：`--mixed`会将工作区和本地分支回退，`git status`状态为`Changed not staged`，需要被`add`进`stage`区(暂存区)
 - `--hard`:将`index`区和工作区同时回退，工作区中自从`<commit>`版本起的所有修改都会被丢弃(也就是将工作区恢复到`<commit>`版本)
总结一下：`--hard`会将工作区和本地分支回退，并将工作区的修改丢弃，此时工作区是`clean`的
 - `--merge`:重置`index`区并将工作区中`<commit>`和`HEAD`版本不一致的文件更新，但是会保留那些工作区中还没有`add`到暂存区的文件(仅当`index`区和`HEAD`以及`<commit>`的版本一样，参考下面第六个表)，如果`<commit>`和`index`区版本不同的文件还没有`add`进`index`区，那么`reset`操作将禁止(`If a file that is deifferent between <commit> and the index has unstaged changes,reset is aborted`，前提是`<commit>`和`index`区版本不一样，再判断工作区是否有`unstaged`的文件)
总结一下：
1、更新`index`区，更新工作区中`<commit>`和`HEAD`版本不同的文件，但会保留工作区中和`index`区不同的文件
2、如果该文件版本与`index`区的版本不一样(也就是`unstaged`)，那么此次`reset`操作取消
 - `--keep`:将暂存区重置并将工作区中`<commit>`和`HEAD`版本不一致的文件更新(言外之意如果`<commit>`和`HEAD`版本相同，那么保持工作区不变，只更新`index`区)
总结一下：
1、更新`index`区，更新工作区中`<commit>`和`HEAD`版本不一致的文件
2、如果该文件有本地修改，那么此次`reset`操作取消

以下几个例子：`git reset --option target`
|`--option`|`working`|`index`|`HEAD`|`target`|
|:-:|:-:|:-:|:-:|:-:|
||`A`|`B`|`C`|`D`|
|`--soft`|`A`|`B`|`D`||
|`--mixed`|`A`|`D`|`D`||
|`--hard`|`D`|`D`|`D`||
|`--merge`|`disallowed`||||
|`--keep`|`disallowed`||||

|`--option`|`working`|`index`|`HEAD`|`target`|
|:-:|:-:|:-:|:-:|:-:|
||`A`|`B`|`C`|`C`|
|`--soft`|`A`|`B`|`C`||
|`--mixed`|`A`|`C`|`C`||
|`--hard`|`C`|`C`|`C`||
|`--merge`|`disallowed`||||
|`--keep`|`A`|`C`|`C`||

|`--option`|`working`|`index`|`HEAD`|`target`|
|:-:|:-:|:-:|:-:|:-:|
||`B`|`B`|`C`|`D`|
|`--soft`|`B`|`B`|`D`||
|`--mixed`|`B`|`D`|`D`||
|`--hard`|`D`|`D`|`D`||
|`--merge`|`D`|`D`|`D`||
|`--keep`|`disallowed`||||

|`--option`|`working`|`index`|`HEAD`|`target`|
|:-:|:-:|:-:|:-:|:-:|
||`B`|`B`|`C`|`C`|
|`--soft`|`B`|`B`|`C`||
|`--mixed`|`B`|`C`|`C`||
|`--hard`|`C`|`C`|`C`||
|`--merge`|`C`|`C`|`C`||
|`--keep`|`B`|`C`|`C`||

|`--option`|`working`|`index`|`HEAD`|`target`|
|:-:|:-:|:-:|:-:|:-:|
||`B`|`C`|`C`|`D`|
|`--soft`|`B`|`C`|`D`||
|`--mixed`|`B`|`D`|`D`||
|`--hard`|`C`|`D`|`D`||
|`--merge`|`disallowed`||||
|`--keep`|`disallowed`||||

|`--option`|`working`|`index`|`HEAD`|`target`|
|:-:|:-:|:-:|:-:|:-:|
||`B`|`C`|`C`|`C`|
|`--soft`|`B`|`C`|`C`||
|`--mixed`|`B`|`C`|`C`||
|`--hard`|`C`|`C`|`C`||
|`--merge`|`B`|`C`|`C`||
|`--keep`|`B`|`C`|`C`||


  [1]: https://www.liaoxuefeng.com/wiki/0013739516305929606dd18361248578c67b8067c8c017b000
  [2]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/git%E6%B5%81%E7%A8%8B%E5%9B%BE.png
  [3]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/feature%20branch.png
  [4]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/merge--no-ff.png
  [5]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/hotfix-branch.png
  [6]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/tools/git-model.png