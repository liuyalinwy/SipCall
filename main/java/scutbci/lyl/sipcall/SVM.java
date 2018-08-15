package scutbci.lyl.sipcall;

import java.io.IOException;

import libsvm.*;

public class SVM implements Classifier {

    public svm_model model;

    @Override
    public void load(String modelfile) throws IOException {
        model = svm.svm_load_model(modelfile);
    }

    @Override
    public void train(double[][] X, double[] y) {
        svm_problem prob = new svm_problem();
        int la=X[0].length;
        svm_node[][] xx = new svm_node[X.length][X[0].length];
        for (int i=0;i<X.length;i++){
            for(int j=0;j<X[0].length;j++){
                xx[i][j] = new svm_node();
                xx[i][j].index=j+1;
                xx[i][j].value=X[i][j];
            }
        }
        prob.l = X.length;
        prob.x = xx;
        prob.y = y;

        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 100;
        param.eps = 0.00001;
        param.C = 1;

        System.out.println(svm.svm_check_parameter(prob, param));
        model = svm.svm_train(prob, param);
    }

    @Override
    public double predict(double[] x) {
        svm_node[] input = new svm_node[x.length];
        for (int i=0; i<x.length; i++)
        {
            input[i] = new svm_node();
            input[i].index = i+1;
            input[i].value = x[i];
        }
        return svm.svm_predict(model, input);
    }

    @Override
    public double[] predict(double[][] X) {
        double[] out = new double[X.length];
        for (int i=0; i<X.length; i++)
        {
            out[i] = predict(X[i]);
        }
        return out;
    }

    @Override
    public void save(String modelfile) throws IOException {
        svm.svm_save_model(modelfile, model);
    }
}
