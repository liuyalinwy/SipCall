package scutbci.lyl.sipcall;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

class AlgoParam {
    public int fs = 250;
    public int dfs = 5;
    public int epochlen = 150;

    public int numChars = 40;
    public int numChannels = 30;
    public int[] channelSelected = null;
    public int timeStart = 0;
    public int timeStop = 150;
    public int numPoints = 25;
    public int numFeatures = 750;
    public double freqStart = 0.5;
    public double freqStop = 10;
    public double[] coefA = null;
    public double[] coefB = null;
    public SignalProcess.Filter filter;

    public String classifier;
    public String modelfile;
    public String modeldir;
    public Classifier model = null;

    public int lmin = 5;
    public int lmax = 8;
    public double nta = 0.2;

    public void load(String filename)
    {
        File file = new File(filename);
        modeldir = file.getParent();
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(file);
            parseXML(doc);
            numPoints = (int)Math.ceil((double)(timeStop-timeStart)/dfs);
            numFeatures = numChannels*numPoints;
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }
    }

    private void parseXML(Document doc) throws IOException {
        Element root = doc.getRootElement();

        Element item;
        String[] strTemp;
        item = root.getChild("fs"); fs = Integer.parseInt(item.getText());
        item = root.getChild("dfs"); dfs = Integer.parseInt(item.getText());
        item = root.getChild("numChars"); numChars = Integer.parseInt(item.getText());
        item = root.getChild("numChannels"); numChannels = Integer.parseInt(item.getText());
        item = root.getChild("channelSelected");
        strTemp = item.getText().split(" ");
        channelSelected = new int[strTemp.length];
        for (int i=0; i<strTemp.length; i++)
        {
            channelSelected[i] = Integer.parseInt(strTemp[i]);
        }
        item = root.getChild("timeStart"); timeStart = Integer.parseInt(item.getText());
        item = root.getChild("timeStop"); timeStop = Integer.parseInt(item.getText());
        item = root.getChild("freqStart"); freqStart = Double.parseDouble(item.getText());
        item = root.getChild("freqStop"); freqStop = Double.parseDouble(item.getText());
        item = root.getChild("coefA"); strTemp = item.getText().split(" ");
        coefA = new double[strTemp.length];
        for (int i=0; i<strTemp.length; i++)
        {
            coefA[i] = Double.parseDouble(strTemp[i]);
        }
        item = root.getChild("coefB"); strTemp = item.getText().split(" ");
        coefB = new double[strTemp.length];
        for (int i=0; i<strTemp.length; i++)
        {
            coefB[i] = Double.parseDouble(strTemp[i]);
        }
        filter = new SignalProcess.Filter(coefA, coefB);

        item = root.getChild("classifier"); classifier = item.getText();
        item = root.getChild("modelfile"); modelfile = item.getText();
        if (classifier.equalsIgnoreCase("svm")) {
            model = new SVM();
        } else if (classifier.equalsIgnoreCase("flda")) {
            model = new FLDA();
        }
        model.load(modeldir+"/"+modelfile);
    }
}

class AlgoState {
    public int numChars = 40;
    public int numFeatures = 750;
    public int[]  charRepeated;
    public double[][] featureAccumulated;
    public ArrayList<double[][]> featureRounds;
    public ArrayList<Double> fscoreDelta;
    public ArrayList<Integer> roundUse;
    public int charIndex = 0;
    public int roundIndex = 0;
    public int trialIndex = 0;
    public int kr = 0;
    public int reset = 0;

    public AlgoState(int charaters, int features, int maxround) {
        numChars = charaters;
        numFeatures = features;
        charRepeated = new int[charaters];
        featureAccumulated = new double[charaters][features];
        featureRounds = new ArrayList<double[][]>();
        fscoreDelta = new ArrayList<Double>();
        roundUse = new ArrayList<Integer>();
    }

    public void reset()
    {
        featureAccumulated = new double[numChars][numFeatures];
        MatrixX.zeros(charRepeated);
        reset = 0;
    }
}

public class EEGAlgorithm {
    final public static int JOB_INIT = 1;
    final public static int JOB_RESET = 2;
    final public static int JOB_ROUND = 3;
    final public static int JOB_EVALUATE = 4;

    private int mEpochLen;
    private int mEvent = -1;
    private double[][] mEpoch = null;

    private String mModelPath = null;
    private AlgoParam mParam = new AlgoParam();
    private AlgoState mState = null;
    private int mResult = -1;

    public void setModelPath(String path)
    {
        mModelPath = path;
    }

    public void setInput(Epoch epoch) {
        synchronized (this) {
            mEpochLen = epoch.epochlen;
            mEvent = epoch.event;
            mEpoch = new double[epoch.channels][epoch.epochlen];
            for (int i = 0; i < epoch.channels; i++) {
                for (int j = 0; j < epoch.epochlen; j++) {
                    mEpoch[i][j] = epoch.data[j * epoch.channels + i];
                }
            }
        }
    }

    public int getResult() {
        synchronized (this) {
            return mResult;
        }
    }

    public void resetResult() {
        synchronized (this) {
            mResult = -1;
        }
    }

    public void call(int type) {
        synchronized (this) {
            switch (type) {
                case JOB_INIT:
                    init();
                    break;
                case JOB_RESET:
                    reset();
                    break;
                case JOB_ROUND:
                    round();
                    break;
                case JOB_EVALUATE:
                    evaluate();
                    break;
            }
        }
    }

    private void init()
    {
        if (mModelPath != null) {
            mParam.load(mModelPath);
            mState = new AlgoState(mParam.numChars, mParam.numFeatures, mParam.lmax);
        }
    }

    private void reset()
    {
        if (mState != null) {
            mState.reset();
        }
    }

    private void round()
    {
        if((mEvent > 0) && (mEvent <= mParam.numChars)) {
            System.out.println("Process event: "+mEvent+", index: "+mState.charIndex+", round: "+mState.kr);
            // channel selection
            double[][] signalFiltered = new double[mParam.numChannels][mEpochLen];
            for (int i=0; i<mParam.numChannels; i++)
            {
                System.arraycopy(mEpoch[mParam.channelSelected[i]-1], 0, signalFiltered[i], 0, mEpochLen);
            }
            mEpoch = null;
            // filtering
            // mParam.filter.filtfilt(signalFiltered, signalFiltered);
            // time selection
            double[][] signalTime = new double[mParam.numChannels][mParam.timeStop-mParam.timeStart];
            for (int i=0; i<mParam.numChannels; i++)
            {
                signalTime[i] = Arrays.copyOfRange(signalFiltered[i], mParam.timeStart, mParam.timeStop);
            }
            signalFiltered = null;
            // downsampling
            double[][] signalDownsampled = new double[mParam.numChannels][mParam.numPoints];
            for (int i=0; i<mParam.numChannels; i++)
            {
                signalDownsampled[i] = SignalProcess.downsample(signalTime[i], mParam.dfs);
            }
            signalTime = null;
            // normalizing
            MatrixX.zscore(signalDownsampled, signalDownsampled);
            // feature extracting
            double[] feature = new double[mParam.numFeatures];
            for (int i=0; i<mParam.numChannels; i++)
            {
                System.arraycopy(signalDownsampled[i], 0, feature, i*mParam.numPoints, mParam.numPoints);
            }
            MatrixX.plus(mState.featureAccumulated[mEvent-1], feature, mState.featureAccumulated[mEvent-1]);
            mState.charRepeated[mEvent-1] ++;

            mState.charIndex ++;
            if (mState.charIndex >= mParam.numChars) {
                mState.charIndex = 0;
                mState.roundIndex ++;
                evaluate();
            }
        }
    }

    private void evaluate()
    {
        mState.kr ++;
        mState.featureRounds.add(mState.featureAccumulated);
        if (mState.featureRounds.size() > mParam.lmax) {
            double[][] featureOld = mState.featureRounds.remove(0);
            featureOld = null;
        }

        if (mState.kr >= mParam.lmin) {
            if (mState.kr > mParam.lmax) {
                mState.kr = mParam.lmax;
            }

            double[][] featureAveraged = new double[mParam.numChars][mParam.numFeatures];
            for (int i = 0; i < mState.kr; i++) {
                double[][] featureRound = mState.featureRounds.get(mState.featureRounds.size()-i-1);
                MatrixX.plus(featureAveraged, featureRound, featureAveraged);
            }

            for (int i = 0; i < mParam.numChars; i++) {
                if (mState.charRepeated[i] > 0) {
                    MatrixX.divide(featureAveraged[i], MatrixX.norm2(featureAveraged[i]), featureAveraged[i]);
                } else {
                    MatrixX.zeros(featureAveraged[i]);
                }
            }

            double[] fscore_raw = mParam.model.predict(featureAveraged);
            featureAveraged = null;
            int index = MatrixX.max(fscore_raw);
            double[] fscore_normalized = MatrixX.scale(fscore_raw);
            MatrixX.sort(fscore_normalized, false); // descending order
            double fscore_delta = fscore_normalized[0] - fscore_normalized[1];
            mState.fscoreDelta.add(fscore_delta);

            if (mState.kr >= mParam.lmax) {
                mState.trialIndex = mState.trialIndex + 1;
                mState.roundUse.add(mState.kr);
                mState.kr = 0;
                mResult = index;
            } else if (fscore_delta >= mParam.nta) {
                mState.trialIndex = mState.trialIndex + 1;
                mState.roundUse.add(mState.kr);
                mState.kr = 0;
                mResult = index;
            } else
                mResult = -1;
        } else {
            mResult = -1;
        }
        System.out.println("evaluate round: "+mState.kr+", result: "+mResult);
        reset();
    }
}
