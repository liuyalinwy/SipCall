package scutbci.lyl.sipcall;

import java.util.ArrayList;

import Jama.Matrix;

public class SignalProcess {
    public static class Filter {
        private double[] a;
        private double[] b;

        public Filter(double[] a, double[] b)
        {
            setCoef(a, b);
        }

        public void setCoef(double[] a, double[] b)
        {
            this.a = new double[a.length];
            this.b = new double[b.length];
            System.arraycopy(a, 0, this.a, 0, a.length);
            System.arraycopy(b, 0, this.b, 0, b.length);
        }

        public void getCoef(double[] a, double[] b)
        {
            System.arraycopy(this.a, 0, a, 0, this.a.length);
            System.arraycopy(this.b, 0, b, 0, this.b.length);
        }

        public double[] filter(double[] x)
        {
            double[] out = new double[x.length];
            filter(x, out);
            return out;
        }

        public void filter(double[] x, double[] y)
        {
            for(int i=0; i<x.length; i++)
            {
                y[i] = b[0] * x[i];
                for(int j=0; j< a.length && j<i; j++)
                {
                    y[i] = y[i] + x[i-j]*b[j] - y[i-j]*a[j];
                }
            }
        }

        public double[][] filter(double[][] X)
        {
            double[][] out = new double[X.length][X[0].length];
            filter(X, out);
            return out;
        }

        public void filter(double[][] X, double[][] Y)
        {
            for (int i=0; i<X.length; i++)
            {
                filter(X[i], Y[i]);
            }
        }

        public double[][] filtfilt(double[][] X)
        {
            double[][] out = new double[X.length][X[0].length];
            filtfilt(X, out);
            return out;
        }

        public void filtfilt(double[][] X, double[][] Y)
        {
            for (int i=0; i<X.length; i++)
            {
                filtfilt(X[i], Y[i]);
            }
        }

        public double[] filtfilt(double[] x)
        {
            double[] y = new double[x.length];
            filtfilt(x, y);
            return y;
        }

        public void filtfilt(double[] x,double[] y)
        {
            ArrayList<Double> B=new ArrayList<Double>();
            ArrayList<Double> A=new ArrayList<Double>();
            ArrayList<Double> X=new ArrayList<Double>();
            for(int i=0;i<b.length;i++){
                B.add(b[i]);
            }
            for(int i=0;i<a.length;i++){
                A.add(a[i]);
            }
            for(int i=0;i<x.length;i++){
                X.add(x[i]);
            }
            int len = X.size();
            int na = A.size();
            int nb = B.size();
            int nfilt = (nb > na) ? nb : na;
            int nfact = 3 * (nfilt - 1);
            if (len <= nfact)
                throw new RuntimeException("输入数值X长度太小，数据最少是滤波器阶数的三倍");
            resize(B,nfilt, 0);
            resize(A,nfilt, 0);

            ArrayList<Integer> rows = new ArrayList<Integer>();
            ArrayList<Integer> cols = new ArrayList<Integer>();

            add_index_range(rows, 0, nfilt - 2,1);
            if (nfilt > 2)
            {
                add_index_range(rows, 1, nfilt - 2, 1);
                add_index_range(rows, 0, nfilt - 3, 1);
            }
            add_index_const(cols, 0, nfilt - 1);
            if (nfilt > 2)
            {
                add_index_range(cols, 1, nfilt - 2,1);
                add_index_range(cols, 1, nfilt - 2,1);
            }
            int klen = rows.size();
            ArrayList<Double> data = new ArrayList<Double>();
            resize(data,klen, 0);
            data.set(0, 1 + A.get(1));
            int j = 1;
            if (nfilt > 2)
            {
                for (int i = 2; i < nfilt; i++)
                    data.set(j++, A.get(i));
                for (int i = 0; i < nfilt - 2; i++)
                    data.set(j++, 1.0);
                for (int i = 0; i < nfilt - 2; i++)
                    data.set(j++, -1.0);
            }
            ArrayList<Double> leftpad = subvector_reverse(X, nfact, 1);
            changeArray2(leftpad,2*X.get(0));

            ArrayList<Double> rightpad = subvector_reverse(X, len - 2, len - nfact - 1);
            changeArray2(rightpad,2*X.get(len - 1));

            double y0;
            ArrayList<Double> signal1 = new ArrayList<Double>();
            ArrayList<Double> signal2 = new ArrayList<Double>();
            ArrayList<Double> zi = new ArrayList<Double>();
            append_vector(signal1, leftpad);
            append_vector(signal1, X);
            append_vector(signal1, rightpad);

            double [][] sp = Zeros(max_val(rows)+1,max_val(cols)+1);
            for (int k = 0; k < klen; ++k)
                sp[rows.get(k)][cols.get(k)] = data.get(k);
            double[]bb = map(B);
            double[]aa = map(A);
            double[][] invSw = new Matrix(sp).inverse().getArray();
            double[] seg = calc(segment(bb,1,nfilt - 1),bb[0],segment(aa,1,nfilt - 1));
            double[] ZZi = MatrixX.product(invSw,seg);
            resize(zi,ZZi.length,1);

            changeZi(ZZi,zi,signal1.get(0));
            filtfilt(B, A, signal1, signal2, zi);
            reverse(signal2);
            changeZi(ZZi,zi,signal2.get(0));
            filtfilt(B, A, signal2, signal1, zi);
            ArrayList<Double> Y = subvector_reverse(signal1, signal1.size() - nfact - 1, nfact);
            for(int i=0;i<Y.size();i++){
                y[i]=Y.get(i);
            }
        }

        private static void reverse(ArrayList<Double> signal2) {
            int i=0;
            int j=signal2.size()-1;
            while(i<j){
                swap(signal2,i,j);
                i++;
                j--;
            }
        }

        private static void swap(ArrayList<Double> signal2, int i, int j) {
            double temp = signal2.get(j);
            signal2.set(j, signal2.get(i));
            signal2.set(i, temp);
        }

        private static void changeZi(double[] zZi, ArrayList<Double> zi, Double double1) {
            for (int i = 0; i < zZi.length; i++) {
                zi.set(i, zZi[i]*double1);
            }
        }

        private static double[] calc(double[] segment, double d, double[] segment2) {
            double[] ret = new double[segment.length];
            for (int i = 0; i < segment.length; i++) {
                ret[i] = segment[i]-d*segment2[i];
            }
            return ret;
        }

        private static double[] segment(double[] bb, int i, int j) {
            double[]ret=new double[j-i+1];
            for (int k = 0; k < j-i+1; k++) {
                ret[k] = bb[i+k];
            }
            return ret;
        }

        private static double[] map(ArrayList<Double> b) {
            double[] ret = new double[b.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = b.get(i);
            }
            return ret;
        }

        private static double[][] Zeros(int ii, int jj) {
            double [][] sp = new double[ii][jj];
            for (int i = 0; i < ii; i++)
                for (int j = 0; j < jj; j++)
                    sp[i][j] = 0;
            return sp;
        }

        public static void filtfilt(ArrayList<Double> B, ArrayList<Double> A,
                                    ArrayList<Double> X, ArrayList<Double> Y,ArrayList<Double> Zi)
        {
            if (A.size() == 0)
                throw new RuntimeException("A 数组为空！");
            boolean flagA = true;
            for (Double doubleA : A) {
                if (doubleA != 0) {
                    flagA = false;
                }
            }
            if (flagA) {
                throw new RuntimeException("A 数组至少要有一个数不为零！");
            }
            if (A.get(0) == 0) {
                throw new RuntimeException("A 数组第一个元素不能为零！");
            }
            changeArray(A, A.get(0));
            changeArray(B, A.get(0));


            int input_size = X.size();
            int filter_order = max(A.size(),B.size());
            resize(B,filter_order,0);
            resize(A,filter_order,0);
            resize(Zi,filter_order,0);
            resize(Y,input_size,0);

            for (int i = 0; i < input_size; i++) {
                int order = filter_order - 1;
                while(order!=0){
                    if(i>=order)
                        Zi.set(order-1, B.get(order)*X.get(i-order)-A.get(order)*Y.get(i-order)+Zi.get(order));
                    --order;
                }
                Y.set(i, B.get(0)*X.get(i)+Zi.get(0));
            }
            Zi.remove(Zi.size()-1);
        }

        private static void resize(ArrayList<Double> a, int i, double j) {
            if(a.size()>=i)
                return;
            int size = a.size();
            for (int j2 = size; j2 < i; j2++) {
                a.add(j);
            }
        }

        private static int max(int a, int b) {
            if(a > b)
                return a;
            else
                return b;
        }

        static void changeArray(ArrayList<Double> vec, double a0) {
            for (int i = 0; i < vec.size(); i++) {
                vec.set(i, vec.get(i)/a0);
            }
        }

        static void changeArray2(ArrayList<Double> vec, double a0) {
            for (int i = 0; i < vec.size(); i++) {
                vec.set(i, a0-vec.get(i));
            }
        }

        static void add_index_range(ArrayList<Integer> indices, int beg, int end,
                                    int inc) {
            for (int i = beg; i <= end; i += inc)
                indices.add(i);
        }

        static void add_index_const(ArrayList<Integer> indices, int value, int numel) {
            while (numel-- != 0)
                indices.add(value);
        }

        static void append_vector(ArrayList<Double> vec, ArrayList<Double> tail) {
            for (Double doubleitem : tail) {
                vec.add(doubleitem);
            }
        }

        static ArrayList<Double> subvector_reverse(ArrayList<Double> vec,
                                                   int idx_end, int idx_start) {
            ArrayList<Double> resultArrayList = new ArrayList<Double>(idx_end
                    - idx_start + 1);
            for (int i = 0; i < idx_end- idx_start + 1; i++) {
                resultArrayList.add(0.0);
            }
            int endindex = idx_end - idx_start;
            for (int i = idx_start; i <= idx_end; i++)
                resultArrayList.set(endindex--, vec.get(i));
            return resultArrayList;
        }

        static int max_val(ArrayList<Integer> vec) {
            int temp = vec.get(0);
            for (Integer integer : vec) {
                if (temp < integer)
                    temp = integer;
            }
            return temp;
        }
    }
    public static double[] downsample(double[] signal, int dfs)
    {
        int offset = dfs/2;
        int newlen = signal.length/dfs;
        double[] newsignal = new double[newlen];
        for (int i=0; i<newlen; i++)
        {
            newsignal[i] = MatrixX.mean(signal, i*dfs, dfs);//+offset
        }
        return newsignal;
    }

    public static double[][] downsample(double[][] signal, int dfs)
    {
        double[][] newsignal = new double[signal.length][1];
        for (int i=0; i<signal.length; i++)
        {
            newsignal[i] = downsample(signal[i], dfs);
        }
        return newsignal;
    }
}

