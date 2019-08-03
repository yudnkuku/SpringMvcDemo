package offer;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        String[] strArr = s.split(";");
        int[] start = {0,0};
        for (int i = 0; i < strArr.length; i++) {
            move(start, strArr[i]);
        }
        System.out.println(start[0] + "," + start[1]);
    }

    private static void move(int[] start, String way) {
        if (way == null || way.length() < 2) {
            return;
        }
        char c = way.charAt(0);
        String s = way.substring(1);
        boolean isDigital = s.matches("[0-9]{1,2}");
        if (!isDigital) {
            return;
        }
        int step = Integer.valueOf(s);
        switch (c) {
            case 'A':
                start[0] = start[0]-step;
                break;
            case 'D':
                start[0] = start[0]+step;
                break;
            case 'W':
                start[1] = start[1]+step;
                break;
            case 'S':
                start[1] = start[1]-step;
            default:
                return;
        }
    }
}