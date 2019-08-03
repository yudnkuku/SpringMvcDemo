package leetcode;

public class OtherCode {

    /**
     * n的阶乘中有多少位0
     * 思路：实际上是计算因子中所有5的个数
     * @param n
     * @return
     */
    public int trailingZeros(int n) {
        return n == 0 ? 0 : n/5 + trailingZeros(n/5);
    }
}
