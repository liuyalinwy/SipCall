package scutbci.lyl.sipcall;

import java.io.IOException;

public interface Classifier {
    public void train(double[][] X, double[] y);
    public double predict(double[] x);
    public double[] predict(double[][] X);
    public void load(String modelfile) throws IOException;
    public void save(String modelfile) throws IOException;
}
