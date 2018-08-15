package scutbci.lyl.sipcall;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import Jama.Matrix;

public class FLDA implements Classifier {

    public double b = 0;
    public double[] w = null;

    @Override
    public void train(double[][] X, double[] y) {
        train(X,y,1e-4);
    }

    public void train(double[][] X, double[] y, double lambda) {
        int[] idx1 = find(y, 1);
        int[] idx2 = find(y, -1);
        int N1 = idx1.length;
        int N2 = idx2.length;
        int P = X[0].length;

        double[][] X1 = new double[N1][P];
        double[][] X2 = new double[N2][P];
        for (int i=0; i<N1; i++) {
            System.arraycopy(X[idx1[i]], 0, X1[i], 0, P);
        }
        for (int i=0; i<N2; i++) {
            System.arraycopy(X[idx2[i]], 0, X2[i], 0, P);
        }

        double[] mu1 = MatrixX.mean(MatrixX.transpose(X1));
        double[] mu2 = MatrixX.mean(MatrixX.transpose(X2));
        double[][] Sw = MatrixX.cov(MatrixX.transpose(X));
        double[][] invSw = new Matrix(MatrixX.plus(Sw, MatrixX.product(MatrixX.eye(P), lambda))).inverse().getArray();

        this.w = MatrixX.product(invSw, MatrixX.minus(mu1, mu2));
        this.b = -0.5 * MatrixX.dotprod(this.w, MatrixX.plus(mu1, mu2));
    }

    @Override
    public double predict(double[] x) {
        double result = 0;
        for (int i = 0; i < x.length; i++) {
            result += x[i] * w[i];
        }
        result += b;
        return result;
    }

    @Override
    public double[] predict(double[][] X) {
        double[] result = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            result[i] = predict(X[i]);
        }
        return result;
    }

    public void load(double[] w, double b) {
        this.w = new double[w.length];
        System.arraycopy(w, 0, this.w, 0, w.length);
        this.b = b;
    }

    @Override
    public void load(String modelfile) throws IOException {
        File file = new File(modelfile);
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(file);
            Element root = doc.getRootElement();
            String[] strTemp = root.getChild("w").getText().split(" ");
            w = new double[strTemp.length];
            for (int i = 0; i < strTemp.length; i++) {
                w[i] = Double.parseDouble(strTemp[i]);
            }
            b = Double.parseDouble(root.getChild("b").getText());

        } catch (JDOMException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(String modelfile) throws IOException {
        Element root = new Element("model");
        Document doc = new Document(root);
        Element eb = new Element("b");
        eb.setText(String.valueOf(b));
        root.addContent(eb);
        Element ew = new Element("w");
        String strW = Arrays.toString(this.w)
                .replaceAll(", ", " ")
                .replace('[', ' ')
                .replace(']', ' ')
                .trim();
        ew.setText(strW);
        root.addContent(ew);

        File file = new File(modelfile);
        if (file.exists()) { file.delete(); }
        file.createNewFile();
        XMLOutputter out = new XMLOutputter();
        FileOutputStream fos= new FileOutputStream(file);
        out.output(doc, fos);
    }

    public int[] find(double[] x, double value) {
        int count = 0;
        int[] indices = null;
        int[] tmpindices = new int[x.length];
        for (int i=0; i<x.length; i++)
        {
            if (x[i] == value) {
                tmpindices[count] = i;
                count ++;
            }
        }
        if (count > 0) {
            indices = new int[count];
            System.arraycopy(tmpindices, 0, indices, 0, count);
        }
        return indices;
    }
}
