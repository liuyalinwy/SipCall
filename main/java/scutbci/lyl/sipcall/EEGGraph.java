package scutbci.lyl.sipcall;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

class EEGChannel extends LinearLayout {
    private Context context;

    private String mLabel = "";
    private int mPoints = 500;// 2s的500个点
    private int mScale = 1;

    private int mTextColor = Color.BLACK;
    private int mLineColor = Color.BLACK;
    private int mBackColor = Color.WHITE;
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset;
    private XYMultipleSeriesRenderer mRenderer;

    public EEGChannel(Context context) {
        super(context);
        initGraph();
    }

    public EEGChannel(Context context, String label, int points, int scale) {
        super(context);
        setLabel(label);
        setPoints(points);
        setScale(scale);
        initGraph();
    }

    public void setLabel(String label) {
        mLabel = label;
    }
    public void setPoints(int points) {
        mPoints = points;
    }
    public void setScale(int scale)
    {
        mScale = scale;
    }

    public void addPoint(int value) {
        XYSeries series = mDataset.getSeriesAt(0);
        mDataset.removeSeries(series);
        int length = series.getItemCount();
        if (length > mPoints) { length = mPoints; }

        int[] xv = new int[mPoints];
        int[] yv = new int[mPoints];
        for (int i = 0; i < length; i++) {
            xv[i] = (int)series.getX(i) + 1;
            yv[i] = (int)series.getY(i);
        }

        series.clear();
        series.add(0, value/mScale);
        for (int i = 0; i < length; i++) {
            series.add(xv[i], yv[i]);
        }
        mDataset.addSeries(series);
        mChart.repaint();
    }

    public void updateData(int[] data)
    {
        updateData(data, 0, data.length);
    }

    public void updateData(int[] data, int offset, int len)
    {
        XYSeries series = mDataset.getSeriesAt(0);
        mDataset.removeSeries(series);
        if (len > mPoints) { len = mPoints; }

        series.clear();
        for (int i = 0; i < len; i++) {
            series.add(i, data[offset+i]/mScale);
        }
        mDataset.addSeries(series);
        mChart.repaint();
    }

    public void initGraph() {
        setOrientation(LinearLayout.HORIZONTAL);
        LayoutParams lpLabel = new LayoutParams(0, LayoutParams.MATCH_PARENT);
        LayoutParams lpChart = new LayoutParams(0, LayoutParams.MATCH_PARENT);

        TextView tv = new TextView(getContext());
        tv.setText(mLabel);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(mTextColor);
        tv.setBackgroundColor(mBackColor);
        lpLabel.weight = 5;
        addView(tv, lpLabel);

        mDataset = new XYMultipleSeriesDataset();
        mRenderer = new XYMultipleSeriesRenderer();
        setRenderer(mRenderer, 0, mPoints, -1e5, 1e5, mTextColor, mTextColor, mBackColor);

        XYSeries series = new XYSeries(mLabel);
        mDataset.addSeries(series);
        XYSeriesRenderer renderer = buildRenderer(mLineColor, PointStyle.POINT, true);
        mRenderer.addSeriesRenderer(renderer);

        mChart = ChartFactory.getLineChartView(getContext(), mDataset, mRenderer);
        mChart.setBackgroundColor(Color.WHITE);
        lpChart.weight = 95;
        addView(mChart, lpChart);
    }

    protected void setRenderer(XYMultipleSeriesRenderer renderer,
                               double xMin, double xMax, double yMin, double yMax,
                               int axesColor, int labelsColor, int backColor)
    {
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.clearXTextLabels();
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
        renderer.setBackgroundColor(backColor);
        renderer.setMargins(new int[]{0, 0, 0, 0});// 设置空白区大小
        renderer.setMarginsColor(Color.TRANSPARENT);// 设置空白区颜色
        renderer.setPointSize((float) 1);
        renderer.setLabelsTextSize(0);// 设置坐标轴的字体大小
        renderer.setAxisTitleTextSize(0);// 设置坐标轴标题的字体大小
        renderer.setShowAxes(false);// 隐藏坐标轴
        renderer.setShowLegend(false);// 隐藏图例
        renderer.setShowGrid(false); // 隐藏网格
    }

    protected XYSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill)
    {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(color);
        r.setPointStyle(style);
        r.setFillPoints(fill);
        r.setLineWidth(3);
        return r;
    }
}

public class EEGGraph extends LinearLayout {

    private int mChannels = 32;
    private int mPoints = 500;
    private int mScale = 200;
    private int[] mDataSource = null;
    private int[] mDataChannel = null;
    private EEGChannel[] mEEGChannels = null;
    private int mUpdateInterval = 40;
    private Handler mUpdateHandler = new Handler();

    public EEGGraph(Context context) {
        super(context);
    }

    public EEGGraph(Context context, int channels, int points, int scale) {
        super(context);
        init(channels, points, scale);
    }

    public void init(int channels, int points, int scale) {
        mPoints = points;
        mChannels = channels;
        mScale = scale;
        mDataChannel = new int[mPoints];

        setOrientation(LinearLayout.VERTICAL);

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        lp.weight = 1;
        mEEGChannels = new EEGChannel[mChannels];
        for (int i = 0; i < mChannels; i++) {
            mEEGChannels[i] = new EEGChannel(getContext(), "" + (i + 1), mPoints, mScale);
            addView(mEEGChannels[i], lp);
        }
    }

    public void addData(int[] data, int len) {
        if (len > mChannels) len = mChannels;
        for (int i = 0; i < len; i++) {
            mEEGChannels[i].addPoint(data[i]);
        }
    }

    public void setDataSource(int[] ds)
    {
        mDataSource = ds;
    }

    public void update()
    {
        if (mDataSource == null) return;

        for(int i=0; i<mChannels; i++)
        {
            for (int j=0; j<mPoints; j++)
            {
                mDataChannel[j] = mDataSource[j*mChannels+i];
            }
            mEEGChannels[i].updateData(mDataChannel);
        }
    }

    public void startUpdate()
    {
        mUpdateHandler.postDelayed(mUpdateThread, mUpdateInterval);
    }

    public void stopUpdate()
    {
        mUpdateHandler.removeCallbacksAndMessages(null);
    }

    private Runnable mUpdateThread = new Runnable() {
        @Override
        public void run() {
            update();
            mUpdateHandler.postDelayed(this, mUpdateInterval);
        }
    };
}