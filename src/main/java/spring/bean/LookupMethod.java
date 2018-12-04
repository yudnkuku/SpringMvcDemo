package spring.bean;

public abstract class LookupMethod {

    public void print() {
        System.out.println(createLazyBean().toString());
    }

    /**
     * 查找方法格式：public|protected [abstract] return-type method-name(no arguments);
     * @return
     */
    public abstract LazyBean createLazyBean();

    public String getValue() {
        return "Replace Method";
    }
}
