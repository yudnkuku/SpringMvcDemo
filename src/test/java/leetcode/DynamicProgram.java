package leetcode;

import java.util.ArrayList;
import java.util.Set;

/**
 * 动态规划问题合集
 */
public class DynamicProgram {

    /**
     * 每天只能买或者卖出，对于每天的股价只需要知道之前最低买入价格即可知道当天卖出的最大收益，比较每天的最大收益
     * 思路：两种状态：买入和卖出；买之后可以卖出
     * @param prices
     * @return
     */
    public int maxProfit(int[] prices) {
        if (prices.length <= 1) {
            return 0;
        }
        int buy = -prices[0];
        int sell = 0;
        for (int i = 0; i < prices.length; i++) {
            buy = Math.max(buy, -prices[i]); //最低买入价格
            sell = Math.max(sell, prices[i] + buy);  //最高收益
        }
        return sell;
    }

    /**
     * 可以无限次买入和卖出
     * @param prices
     * @return
     */
    public int maxProfit1(int[] prices) {
        if (prices.length <= 1) {
            return 0;
        }
        int buy = -prices[0];
        int sell = 0;
        for (int i = 0; i < prices.length; i++) {
            sell = Math.max(sell, buy+prices[i]);
            buy = Math.max(buy, sell-prices[i]);
        }
        return sell;
    }

    /**
     * 最多允许两次买入卖出
     * @param prices
     * @return
     */
    public int maxProfit2(int[] prices) {
        int firstBuy = Integer.MIN_VALUE;
        int firstSell = 0;
        int secondBuy = Integer.MIN_VALUE;
        int secondSell = 0;
        for (int i = 0; i < prices.length; i++) {
            firstBuy = Math.max(firstBuy, -prices[i]);
            firstSell = Math.max(firstSell, prices[i]+firstBuy);
            secondBuy = Math.max(secondBuy, firstSell-prices[i]);
            secondSell = Math.max(secondSell, prices[i]+secondBuy);
        }
        return secondSell;
    }

    /**
     * 给定一个待拆分字符串s和字典dict，判断s是否能够被拆分为字典中包含的字符串
     * 思路：动态规划，定义一个数组 boolean[] memo，其中第i位memo[i]表示待拆分字符串从0到i-1位能否被拆分
     * @param s
     * @param dict
     * @return
     */
    public boolean wordBreak(String s, Set<String> dict) {
        if (null == s) {
            return false;
        }
        int n = s.length();
        int max = 0;
        for (String str : dict) {
            max = str.length() > max ? str.length() : max;
        }
        boolean[] memo = new boolean[n+1];
        memo[0] = true;

        for (int i = 1; i <= n; i++) {
            for (int j = i-1; j >= 0 && i-j <= max; j--) {
                //如果memo[j] == true && dict.contains(s.substring(j, i)) => memo[i] = true
                if (memo[j] && dict.contains(s.substring(j, i))) {
                    memo[i] = true;
                    break;
                }
            }
        }
        return memo[n];
    }

    public ArrayList<String> wordBreak2(String s, Set<String> dict) {
        return null;
    }



}
