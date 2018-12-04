package demo.conpro;

public class Producer extends Thread {

    //每次要生产的产品数量
    private int num;

    //仓库对象
    private Storage storage;

    public Producer(Storage storage,String tName) {
        super(tName);
        this.storage = storage;
    }

    @Override
    public void run() {
        storage.produce(num);
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
