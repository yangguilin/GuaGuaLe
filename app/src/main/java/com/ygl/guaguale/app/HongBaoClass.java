package com.ygl.guaguale.app;

import java.util.Random;

/**
 * 红包管理类
 * Created by yanggavin on 14-4-1.
 */
public class HongBaoClass {

    private static float _winPercent = 0.3f;    // 中奖率，0.2=20%
    private static final int _baseNum = 10;     // 随机数基数范围

    /**
     * 获取随机奖金
     *
     * @return 奖金数额
     */
    public static int GetRandomValue() {
        int prize = 0;
        Boolean win = false;

        Random r = new Random(System.currentTimeMillis());

        int val = r.nextInt(_baseNum);
        if (val < (_baseNum * _winPercent)) {
            win = true;
        }

        if (win) {
            Random r2 = new Random(System.currentTimeMillis());
            int val2 = r2.nextInt(_baseNum);

            switch (val2) {
                case 8:
                    prize = 100;
                    break;
                case 7:
                case 6:
                    prize = 50;
                    break;
                case 5:
                case 4:
                case 3:
                    prize = 10;
                    break;
                default:
                    prize = 1;
                    break;
            }
        }

        return prize;
    }
}