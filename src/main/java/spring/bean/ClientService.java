package spring.bean;

public class ClientService {

    private static ClientService instance = new ClientService("bean1");

    public ClientService() {}

    public ClientService(String arg) {
        System.out.println(arg);
    }
    public static ClientService newInstance() {
        return instance;
    }

    public void print() {
        System.out.println("print method");
    }

    public ClientService createInstance() {
        return new ClientService("bean2");
    }
}
