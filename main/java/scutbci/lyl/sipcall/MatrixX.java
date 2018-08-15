package scutbci.lyl.sipcall;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MatrixX {

    public static double[] clone(double[] a)
    {
        double[] out = new double[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }

    public static double[][] clone(double[][] a)
    {
        double[][] out = new double[a.length][a[0].length];
        for (int i=0; i<a.length; i++)
        {
            System.arraycopy(a[i], 0, out[i], 0, a[0].length);
        }
        return out;
    }

    public static double[] plus(double[] a, double b)
    {
        double[] c = new double[a.length];
        plus(a, b, c);
        return c;
    }

    public static void plus(double[] a, double b, double[] c)
    {
        for (int i=0; i<a.length; i++)
        {
            c[i] = a[i] + b;
        }
    }

    public static double[] plus(double[] a, double[] b)
    {
        double[] c = new double[a.length];
        plus(a, b, c);
        return c;
    }

    public static void plus(double[] a, double[] b, double[] c)
    {
        for (int i=0; i<a.length; i++)
        {
            c[i] = a[i] + b[i];
        }
    }

    public static double[][] plus(double[][] a, double b)
    {
        double[][] c = new double[a.length][a[0].length];
        plus(a, b, c);
        return c;
    }

    public static void plus(double[][] a, double b, double[][] c)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                c[i][j] = a[i][j] + b;
            }
        }
    }

    public static double[][] plus(double[][] a, double[][] b)
    {
        double[][] c = new double[a.length][a[0].length];
        plus(a, b, c);
        return c;
    }

    public static void plus(double[][] a, double[][] b, double[][] c)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                c[i][j] = a[i][j] + b[i][j];
            }
        }
    }

    public static double[] minus(double[] a, double b)
    {
        double[] c = new double[a.length];
        minus(a, b, c);
        return c;
    }

    public static void minus(double[] a, double b, double[] c)
    {
        for (int i=0; i<a.length; i++)
        {
            c[i] = a[i] - b;
        }
    }

    public static double[] minus(double[] a, double[] b)
    {
        double[] c = new double[a.length];
        minus(a, b, c);
        return c;
    }

    public static void minus(double[] a, double[] b, double[] c)
    {
        for (int i=0; i<a.length; i++)
        {
            c[i] = a[i] - b[i];
        }
    }

    public static double[][] minus(double[][] a, double b)
    {
        double[][] c = new double[a.length][a[0].length];
        minus(a, b, c);
        return c;
    }

    public static void minus(double[][] a, double b, double[][] c)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                c[i][j] = a[i][j] - b;
            }
        }
    }

    public static double[][] minus(double[][] a, double[][] b)
    {
        double[][] c = new double[a.length][a[0].length];
        minus(a, b, c);
        return c;
    }

    public static void minus(double[][] a, double[][] b, double[][] c)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                c[i][j] = a[i][j] - b[i][j];
            }
        }
    }

    public static double[] product(double[] a, double b)
    {
        double[] c = new double[a.length];
        product(a, b, c);
        return c;
    }

    public static void product(double[] a, double b, double[] c)
    {
        for (int i=0; i<a.length; i++)
        {
            c[i] = a[i] * b;
        }
    }

    public static double[][] product(double[][] a, double b)
    {
        double[][] c = new double[a.length][a[0].length];
        product(a, b, c);
        return c;
    }

    public static void product(double[][] a, double b, double[][] c)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                c[i][j] = a[i][j] * b;
            }
        }
    }

    public static double[] product(double[][] a, double[] b)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++)
        {
            c[i] = dotprod(a[i],b);
        }
        return c;
    }

    public static double[][] product(double[][] a, double[][] b)
    {
        double[][] c = new double[a.length][b.length];
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<b.length; j++)
            {
                c[i][j] = dotprod(a[i], b[j]);
            }
        }
        return c;
    }

    public static double[] divide(double[] a, double b)
    {
        double[] c = new double[a.length];
        divide(a, b, c);
        return c;
    }

    public static void divide(double[] a, double b, double[] c)
    {
        for (int i=0; i<a.length; i++)
        {
            c[i] = a[i] / b;
        }
    }

    public static double[][] divide(double[][] a, double b)
    {
        double[][] c = new double[a.length][a[0].length];
        divide(a, b, c);
        return c;
    }

    public static void divide(double[][] a, double b, double[][] c)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                c[i][j] = a[i][j] / b;
            }
        }
    }

    public static double dotprod(double[] a, double[] b)
    {
        double out = 0;
        for (int i=0; i<a.length; i++)
        {
            out += a[i]*b[i];
        }
        return out;
    }

    public static double dotprod(double[][] a, double[][] b)
    {
        double out = 0;
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                out += a[i][j] * b[i][j];
            }
        }
        return out;
    }

    public static void zeros(int[] a)
    {
        for (int i=0; i<a.length; i++)
        {
            a[i] = 0;
        }
    }

    public static void zeros(int[][] a)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                a[i][j] = 0;
            }
        }
    }

    public static double[] zeros(int n)
    {
        double[] out = new double[n];
        zeros(out);
        return out;
    }

    public static double[][] zeros(int m, int n)
    {
        double[][] out = new double[m][n];
        zeros(out);
        return out;
    }

    public static void zeros(double[] a)
    {
        for (int i=0; i<a.length; i++)
        {
            a[i] = 0;
        }
    }

    public static void zeros(double[][] a)
    {
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                a[i][j] = 0;
            }
        }
    }

    public static double[][] eye(int n)
    {
        double[][] out = new double[n][n];
        for (int i=0; i<n; i++)
        {
            out[i][i] = 1;
        }
        return out;
    }

    public static double[][] transpose(double[][] a)
    {
        double[][] out = new double[a[0].length][a.length];
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a[0].length; j++)
            {
                out[j][i] = a[i][j];
            }
        }
        return out;
    }

    public static double norm2(double[] a)
    {
        double out = 0;
        for (int i=0; i<a.length; i++)
        {
            out += a[i]*a[i];
        }
        out = Math.sqrt(out);
        return out;
    }

    public static double mean(double[] a)
    {
        double out = 0;
        for (int i=0; i<a.length; i++)
        {
            out += a[i];
        }
        out /= a.length;
        return out;
    }

    public static double mean(double[] a, int offset, int len)
    {
        double out = 0;
        for (int i=0; i<len; i++)
        {
            out += a[offset+i];
        }
        out /= len;
        return out;
    }

    public static double[] mean(double[][] a)
    {
        double[] out = new double[a.length];
        for (int i=0; i<a.length; i++)
        {
            out[i] = mean(a[i]);
        }
        return out;
    }

    public static double std(double[] a)
    {
        double out = 0;
        double ma = mean(a);
        for (int i=0; i<a.length; i++)
        {
            out += (a[i]-ma)*(a[i]-ma);
        }
        out = Math.sqrt(out/(a.length - 1));
        return out;
    }

    public static double cov(double[] a, double[] b)
    {
        double out = 0;
        double ma = mean(a);
        double mb = mean(b);
        for (int i=0; i<a.length; i++)
        {
            out += (a[i]-ma)*(b[i]-mb);
        }
        out /= (a.length - 1);
        return out;
    }

    public static double[][] cov(double[][] a)
    {
        double[][] out = new double[a.length][a.length];
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<a.length; j++)
            {
                out[i][j] += cov(a[i],a[j]);
            }
        }
        return out;
    }

    public static double[][] cov(double[][] a, double[][] b)
    {
        double[][] out = new double[a.length][b.length];
        for (int i=0; i<a.length; i++)
        {
            for (int j=0; j<b.length; j++)
            {
                out[i][j] += cov(a[i],b[j]);
            }
        }
        return out;
    }

    public static double[] abs(double[] a)
    {
        double[] b = new double[a.length];
        abs(a, b);
        return b;
    }

    public static void abs(double[] a, double[] b)
    {
        for (int i=0; i<a.length; i++)
        {
            b[i] = Math.abs(a[i]);
        }
    }

    public static double[][] abs(double[][] a)
    {
        double[][] b = new double[a.length][a[0].length];
        abs(a, b);
        return b;
    }

    public static void abs(double[][] a, double[][] b)
    {
        for (int i=0; i<a.length; i++)
        {
            abs(a[i], b[i]);
        }
    }

    public static double[] zscore(double[] a)
    {
        double[] b = new double[a.length];
        zscore(a, b);
        return b;
    }

    public static void zscore(double[] a, double[] b)
    {
        double ma = mean(a);
        double stda = std(a);
        for (int i=0; i<a.length; i++)
        {
            b[i] = (a[i]-ma)/stda;
        }
    }

    public static double[][] zscore(double[][] a)
    {
        double[][] b = new double[a.length][a[0].length];
        zscore(a, b);
        return b;
    }

    public static void zscore(double[][] a, double[][] b)
    {
        for (int i=0; i<a.length; i++)
        {
            zscore(a[i], b[i]);
        }
    }

    public static double[] scale(double[] a)
    {
        double[] b = new double[a.length];
        scale(a, b);
        return b;
    }

    public static void scale(double[] a, double[] b)
    {
        double maxv = a[max(a)];
        double minv = a[min(a)];
        for (int i=0; i<a.length; i++)
        {
            b[i] = (a[i]-minv)/(maxv-minv);
        }
    }

    public static double[][] scale(double[][] a)
    {
        double[][] b = new double[a.length][a[0].length];
        scale(a, b);
        return b;
    }

    public static void scale(double[][] a, double[][] b)
    {
        for (int i=0; i<a.length; i++)
        {
            scale(a[i], b[i]);
        }
    }

    public static int[] sort(double[] a)
    {
        return sort(a, true);
    }

    public static int[] sort(double[] a, boolean ascend)
    {
        int[] idx = new int[a.length];
        for (int i=0; i<a.length; i++)
            idx[i] = i;

        int temp1;
        double temp2;
        for (int i=0; i < a.length-1; i++)
            for (int j = i+1; j < a.length; j++)
                if (ascend) {
                    if (a[i] > a[j]) {
                        temp1 = idx[i]; idx[i] = idx[j]; idx[j] = temp1;
                        temp2 = a[i]; a[i] = a[j]; a[j] = temp2;
                    }
                } else {
                    if (a[i] < a[j]) {
                        temp1 = idx[i]; idx[i] = idx[j]; idx[j] = temp1;
                        temp2 = a[i]; a[i] = a[j]; a[j] = temp2;
                    }
                }
        return idx;
    }

    public static int max(double[] a)
    {
        int out = 0;
        double maxvalue = a[out];
        for (int i=1; i<a.length; i++)
        {
            if (a[i] > maxvalue) {
                out = i;
                maxvalue = a[out];
            }
        }
        return out;
    }

    public static int min(double[] a)
    {
        int out = 0;
        double minvalue = a[out];
        for (int i=1; i<a.length; i++)
        {
            if (a[i] < minvalue) {
                out = i;
                minvalue = a[out];
            }
        }
        return out;
    }

    public void write(int[][] data, int r, int c, OutputStream os) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                bw.write(String.format("%d, ",data[i][j]));
            }
            bw.write("\n");
        }
        bw.flush();
    }

    public void read(int[][] data, int r, int c, InputStream is) throws IOException
    {
        if (data == null)  data = new int[r][c];
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        int i = 0;
        String strLine = null;
        while ((strLine = br.readLine()) != null) {
            String[] strData = strLine.split("\\d");
            for (int j=0; j<strData.length; j++)
            {
                data[i][j] = Integer.parseInt(strData[j]);
            }
            i++;
        }
    }

    public void write(int[] data, int r, int c, OutputStream os) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                bw.write(String.format("%d, ",data[i*c+j]));
            }
            bw.write("\n");
        }
        bw.flush();
    }

    public void read(int[] data, int r, int c, InputStream is) throws IOException
    {
        if (data == null)  data = new int[r*c];
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        int i = 0;
        String strLine = null;
        while ((strLine = br.readLine()) != null) {
            String[] strData = strLine.split("\\d");
            for (int j=0; j<strData.length; j++)
            {
                data[i*c+j] = Integer.parseInt(strData[j]);
            }
            i++;
        }
    }

    public void write(double[][] data, int r, int c, OutputStream os) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                bw.write(String.format("%.4f, ",data[i][j]));
            }
            bw.write("\n");
        }
        bw.flush();
    }

    public void read(double[][] data, int r, int c, InputStream is) throws IOException
    {
        if (data == null)  data = new double[r][c];
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        int i = 0;
        String strLine = null;
        while ((strLine = br.readLine()) != null) {
            String[] strData = strLine.split("\\d");
            for (int j=0; j<strData.length; j++)
            {
                data[i][j] = Double.parseDouble(strData[j]);
            }
            i++;
        }
    }

    public void write(double[] data, int r, int c, OutputStream os) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                bw.write(String.format("%.4f, ",data[i*c+j]));
            }
            bw.write("\n");
        }
        bw.flush();
    }

    public void read(double[] data, int r, int c, InputStream is) throws IOException
    {
        if (data == null)  data = new double[r*c];
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        int i = 0;
        String strLine = null;
        while ((strLine = br.readLine()) != null) {
            String[] strData = strLine.split("\\d");
            for (int j=0; j<strData.length; j++)
            {
                data[i*c+j] = Double.parseDouble(strData[j]);
            }
            i++;
        }
    }

    public void print(int[][] data, int r, int c)
    {
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                System.out.print(String.format("%d, ",data[i][j]));
            }
            System.out.print("\n");
        }
    }

    public void print(int[] data, int r, int c)
    {
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                System.out.print(String.format("%d, ",data[i*c+j]));
            }
            System.out.print("\n");
        }
    }

    public void print(double[][] data, int r, int c)
    {
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                System.out.print(String.format("%.4f, ",data[i][j]));
            }
            System.out.print("\n");
        }
    }

    public void print(double[] data, int r, int c)
    {
        for (int i=0; i<r; i++)
        {
            for (int j=0; j<c; j++)
            {
                System.out.print(String.format("%.4f, ",data[i*c+j]));
            }
            System.out.print("\n");
        }
    }
}
