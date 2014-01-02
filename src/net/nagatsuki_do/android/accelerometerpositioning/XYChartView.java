package net.nagatsuki_do.android.accelerometerpositioning;

import org.afree.chart.AFreeChart;
import org.afree.chart.ChartFactory;
import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import android.content.Context;
import android.util.AttributeSet;

public class XYChartView extends ChartView {
	private AFreeChart chart = null;
	private XYSeriesCollection dataset = null;

	public XYChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void createChart() {
        // データのセット
        dataset = new XYSeriesCollection(new XYSeries("x"));
        dataset.addSeries(new XYSeries("y"));
        dataset.addSeries(new XYSeries("z"));
        dataset.setAutoWidth(true);

        chart = ChartFactory.createTimeSeriesChart("Acclerometer", "ms", "加速度", dataset, true, false, false);

        setChart(chart);
        chart.removeChangeListener(this);
    }

	private int points = 1000;
	public void setChartPoints(int points) {
		this.points = points;
	}

	public void addPoint(int seriesIdx, double x, double y) {
		XYSeries series = dataset.getSeries(seriesIdx);
		series.add(x, y);
		
		if (series.getItemCount() > points) series.remove(0);
	}
	
	public void seriesChanged() {
		dataset.seriesChanged(null);
	}
	
	public void reset() {
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			dataset.getSeries(i).clear();
		}
	}
}
