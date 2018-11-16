package com.lib.utils;




public class RSCode {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        RSCode rs = new RSCode();

        //输入要编码的数据
        String input = "abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxy12345678902";
        //System.arraycopy(input.getBytes(),0, rs.data,0,111);

        for(int i = 0; i < 111; i++){
            rs.data[i] = input.getBytes()[i];
        }
//        rs.data[1] = 127;
        rs.rsEncode();

        for(int i=0; i<NN-KK; i++)
            rs.recd[i] = rs.bb[i];
        for(int i=0; i<KK; i++)
            rs.recd[i+NN-KK] = rs.data[i];

        //主动弄点错误
        for(int i=10; i<16; i++)
            rs.recd[i] = 1;

        //解码，纠错
        rs.rsDecode();

        //输出正确编码和纠错后的编码
        System.out.println("i  data  recd");
        for(int i=0; i<NN-KK; i++) {
            System.out.println(i + "   " + rs.bb[i] + "   " + rs.recd[i]);
        }
        for(int i=NN-KK; i<NN; i++) {
            System.out.println(i + "   " + rs.data[i-NN+KK] + "   " + rs.recd[i]);
        }
    }

    private static final int MM = 7;
    private static final int NN = 127;
    private static final int TT = 16;
    private static final int KK = 111;

    private byte[] pp = {1, 0, 1, 1, 1, 0, 0, 0, 1};
    private int[] alphaTo = new int[NN + 1];
    private int[] indexOf = new int[NN + 1];
    private int[] gg = new int[NN - KK + 1];
    public int[] recd = new int[NN];
    public int[] data = new int[KK];
    public int[] bb = new int[NN - KK];

    //NN为报文的最大位数，kk为原文  NN-KK是监督位数目，纠错能力为（NN-KK）/2

    /**
     * 构造函数RSCode()
     * 初始化工作，生成GF空间和对应的生成多项式
     */
    public RSCode() {
        generateGF();
        generatePolynomial();
    }

    /**
     * 函数generateGF()
     * 生成GF(2^MM)空间
     */
    public void generateGF() {
        int i, mask;
        mask = 1;
        alphaTo[MM] = 0;
        for (i = 0; i < MM; i++) {
            alphaTo[i] = mask;
            indexOf[alphaTo[i]] = i;
            if (pp[i] != 0) {
                alphaTo[MM] ^= mask;
            }
            mask <<= 1;
        }

        indexOf[alphaTo[MM]] = MM;
        mask >>= 1;

        for (i = MM + 1; i < NN; i++) {
            if (alphaTo[i - 1] >= mask) {
                alphaTo[i] = alphaTo[MM] ^ ((alphaTo[i - 1] ^ mask) << 1);
            } else {
                alphaTo[i] = alphaTo[i - 1] << 1;
            }
            indexOf[alphaTo[i]] = i;
        }

        indexOf[0] = -1;
        //输出GF空间
/*		System.out.println("GF空间:");
		for(i=0; i<=NN; i++){
			System.out.println(i + "   " + alphaTo[i] + "   " + indexOf[i]);
		}*/
    }//GenerateGF

    /**
     * 函数generatePolynomial()
     * 产生相应的生成多项式的各项系数
     */
    public void generatePolynomial() {
        int i, j;
        gg[0] = 2;
        gg[1] = 1;
        for (i = 2; i <= NN - KK; i++) {
            gg[i] = 1;
            for (j = i - 1; j > 0; j--) {
                if (gg[j] != 0)
                    gg[j] = gg[j - 1] ^ alphaTo[(indexOf[gg[j]] + i) % NN];
                else
                    gg[j] = gg[j - 1];
            }
            gg[0] = alphaTo[(indexOf[gg[0]] + i) % NN];
        }

        //转换其到
        for (i = 0; i <= NN - KK; i++) {
            gg[i] = indexOf[gg[i]];
        }

        //输出生成多项式的各项系数
        System.out.println("生成多项式系数:");
        for (i = 0; i <= NN - KK; i++) {
            System.out.println(gg[i]);
        }
    }

    /**
     * 函数rsEncode()
     * RS编码
     */
    public void rsEncode() {
        int i, j;
        int feedback;
        //NN-KK为监督位
        for (i = 0; i < NN - KK; i++) {
            bb[i] = 0;
        }
        for (i = KK - 1; i >= 0; i--) {
            //逐步的将下一步要减的，存入bb(i)
            feedback = indexOf[data[i] ^ bb[NN - KK - 1]];
            if (feedback != -1) {
                for (j = NN - KK - 1; j > 0; j--) {
                    if (gg[j] != -1)
                        bb[j] = bb[j - 1] ^ alphaTo[(gg[j] + feedback) % NN];
                    else
                        bb[j] = bb[j - 1];
                }
                bb[0] = alphaTo[(gg[0] + feedback) % NN];
            } else {
                for (j = NN - KK - 1; j > 0; j--) {
                    bb[j] = bb[j - 1];
                }
                bb[0] = 0;
            }
        }
        //输出编码结果
        System.out.println("编码结果:");
        for (i = 0; i < NN - KK; i++) {
            System.out.println(bb[i]);
        }
    }


    /**
     * 函数rsDecode()
     * RS解码
     */


    public void rsDecode() {
        int i, j, u, q;
        int[][] elp = new int[NN - KK + 2][NN - KK];
        int[] d = new int[NN - KK + 2];
        int[] l = new int[NN - KK + 2];
        int[] u_lu = new int[NN - KK + 2];
        int[] s = new int[NN - KK + 1];

        int count = 0;
        int syn_error = 0;
        int[] root = new int[TT];
        int[] loc = new int[TT];
        int[] z = new int[TT + 1];
        int[] err = new int[NN];
        int[] reg = new int[TT + 1];

        //转换成GF空间
        for (i = 0; i < NN; i++) {
            if (recd[i] == -1)
                recd[i] = 0;
            else
                recd[i] = indexOf[recd[i]];
        }

        //求伴随多项式
        for (i = 1; i <= NN - KK; i++) {
            s[i] = 0;
            for (j = 0; j < NN; j++) {
                if (recd[j] != -1)
                    s[i] ^= alphaTo[(recd[j] + i * j) % NN];
            }
            if (s[i] != 0)
                syn_error = 1;
            s[i] = indexOf[s[i]];
        }
        System.out.println("syn_error=" + syn_error);

        //如果有错，则进行纠正
        if (syn_error == 1) {
            //BM迭代求错误多项式的系数
            d[0] = 0;
            d[1] = s[1];
            elp[0][0] = 0;
            elp[1][0] = 1;
            for (i = 1; i < NN - KK; i++) {
                elp[0][i] = -1;
                elp[1][i] = 0;
            }
            l[0] = 0;
            l[1] = 0;
            u_lu[0] = -1;
            u_lu[1] = 0;
            u = 0;
            do {
                u++;
                if (d[u] == -1) {
                    l[u + 1] = l[u];
                    for (i = 0; i <= l[u]; i++) {
                        elp[u + 1][i] = elp[u][i];
                        elp[u][i] = indexOf[elp[u][i]];
                    }
                } else {
                    q = u - 1;
                    while ((d[q] == -1) && (q > 0)) {
                        q--;
                    }
                    if (q > 0) {
                        j = q;
                        do {
                            j--;
                            if ((d[j] != -1) && (u_lu[q] < u_lu[j])) {
                                q = j;
                            }
                        } while (j > 0);
                    }

                    if (l[u] > l[q] + u - q) {
                        l[u + 1] = l[u];
                    } else {
                        l[u + 1] = l[q] + u - q;
                    }

                    for (i = 0; i < NN - KK; i++) {
                        elp[u + 1][i] = 0;
                    }
                    for (i = 0; i <= l[q]; i++) {
                        if (elp[q][i] != -1)
                            elp[u + 1][i + u - q] = alphaTo[(d[u] + NN - d[q] + elp[q][i]) % NN];
                    }

                    for (i = 0; i <= l[u]; i++) {
                        elp[u + 1][i] ^= elp[u][i];
                        elp[u][i] = indexOf[elp[u][i]];
                    }
                }
                u_lu[u + 1] = u - l[u + 1];

                if (u < NN - KK) {
                    if (s[u + 1] != -1) {
                        d[u + 1] = alphaTo[s[u + 1]];
                    } else {
                        d[u + 1] = 0;
                    }

                    for (i = 1; i <= l[u + 1]; i++) {
                        if ((s[u + 1 - i] != -1) && (elp[u + 1][i] != 0)) {
                            d[u + 1] ^= alphaTo[(s[u + 1 - i] + indexOf[elp[u + 1][i]]) % NN];
                        }
                    }
                    d[u + 1] = indexOf[d[u + 1]];
                }
            } while ((u < NN - KK) && (l[u + 1] <= TT));
            u++;
            System.out.println("错误数目:" + l[u]);

            //求错误位置，以及改正错误
            if (l[u] <= TT) {
                for (i = 0; i <= l[u]; i++) {
                    elp[u][i] = indexOf[elp[u][i]];
                }
                //求错误位置多项式的根
                for (i = 1; i <= l[u]; i++) {
                    reg[i] = elp[u][i];
                }
                count = 0;
                for (i = 1; i <= NN; i++) {
                    q = 1;
                    for (j = 1; j <= l[u]; j++) {
                        if (reg[j] != -1) {
                            reg[j] = (reg[j] + j) % NN;
                            q ^= alphaTo[reg[j]];
                        }
                    }

                    if (q == 0) {
                        root[count] = i;
                        loc[count] = NN - i;
                        System.out.println("错误位置:" + loc[count]);
                        count++;
                    }
                }

                //
                if (count == l[u]) {
                    for (i = 1; i <= l[u]; i++) {
                        if ((s[i] != -1) && elp[u][i] != -1) {
                            z[i] = alphaTo[s[i]] ^ alphaTo[elp[u][i]];
                        } else if ((s[i] != -1) && (elp[u][i] == -1)) {
                            z[i] = alphaTo[s[i]];
                        } else if ((s[i] == -1) && (elp[u][i] != -1)) {
                            z[i] = alphaTo[elp[u][i]];
                        } else {
                            z[i] = 0;
                        }

                        for (j = 1; j < i; j++) {
                            if ((s[j] != -1) && (elp[u][i - j] != -1)) {
                                z[i] ^= alphaTo[(elp[u][i - j] + s[j]) % NN];
                            }
                        }

                        z[i] = indexOf[z[i]];
                    }

                    //计算错误图样
                    for (i = 0; i < NN; i++) {
                        err[i] = 0;
                        if (recd[i] != -1)
                            recd[i] = alphaTo[recd[i]];
                        else
                            recd[i] = 0;
                    }
                    for (i = 0; i < l[u]; i++) {
                        err[loc[i]] = 1;
                        for (j = 1; j <= l[u]; j++) {
                            if (z[j] != -1)
                                err[loc[i]] ^= alphaTo[(z[j] + j * root[i]) % NN];
                        }

                        if (err[loc[i]] != 0) {
                            err[loc[i]] = indexOf[err[loc[i]]];
                            q = 0;
                            for (j = 0; j < l[u]; j++) {
                                if (j != i)
                                    q += indexOf[1 ^ alphaTo[(loc[j] + root[i]) % NN]];
                            }
                            q = q % NN;
                            err[loc[i]] = alphaTo[(err[loc[i]] - q + NN) % NN];
                            recd[loc[i]] ^= err[loc[i]];
                        }
                    }
                } else {
                    //错误太多，无法改正
                    for (i = 0; i < NN; i++) {
                        if (recd[i] != -1)
                            recd[i] = alphaTo[recd[i]];
                        else
                            recd[i] = 0;
                    }
                }
            } else {
                //错误太多，无法改正
                for (i = 0; i < NN; i++) {
                    if (recd[i] != -1)
                        recd[i] = alphaTo[recd[i]];
                    else
                        recd[i] = 0;
                }
            }
        } else {
            for (i = 0; i < NN; i++) {
                if (recd[i] != -1)
                    recd[i] = alphaTo[recd[i]];
                else
                    recd[i] = 0;
            }
        }
    }
}