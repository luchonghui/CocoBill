package com.copasso.cocobill.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.bigkoo.pickerview.TimePickerView;
import com.copasso.cocobill.R;
import com.copasso.cocobill.adapter.MonthChartAdapter;
import com.copasso.cocobill.bean.MonthChartBean;
import com.copasso.cocobill.bean.MonthDetailBean;
import com.copasso.cocobill.utils.*;
import com.copasso.cocobill.view.CircleImageView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.copasso.cocobill.utils.DateUtils.FORMAT_M;
import static com.copasso.cocobill.utils.DateUtils.FORMAT_Y;

/**
 * 类别报表
 */
public class MenuChartFragment extends BaseFragment
        implements OnChartValueSelectedListener {


    @BindView(R.id.chart)
    PieChart mChart;
    @BindView(R.id.center_title)
    TextView centerTitle;
    @BindView(R.id.center_money)
    TextView centerMoney;
    @BindView(R.id.layout_center)
    LinearLayout layoutCenter;
    @BindView(R.id.center_img)
    ImageView centerImg;
    @BindView(R.id.data_year)
    TextView dataYear;
    @BindView(R.id.data_month)
    TextView dataMonth;
    @BindView(R.id.layout_data)
    LinearLayout layoutData;
    @BindView(R.id.t_outcome)
    TextView tOutcome;
    @BindView(R.id.t_income)
    TextView tIncome;
    @BindView(R.id.circle_bg)
    CircleImageView circleBg;
    @BindView(R.id.circle_img)
    ImageView circleImg;
    @BindView(R.id.layout_circle)
    RelativeLayout layoutCircle;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.money)
    TextView money;
    @BindView(R.id.rank_title)
    TextView rankTitle;
    @BindView(R.id.layout_other)
    RelativeLayout layoutOther;
    @BindView(R.id.other_money)
    TextView otherMoney;
    @BindView(R.id.swipe)
    SwipeRefreshLayout swipe;
    @BindView(R.id.item_type)
    RelativeLayout itemType;
    @BindView(R.id.item_other)
    RelativeLayout itemOther;
    @BindView(R.id.rv_list)
    RecyclerView rvList;
    @BindView(R.id.layout_typedata)
    LinearLayout layoutTypedata;

    private boolean TYPE = true;//默认总收入true
    private List<MonthChartBean.SortTypeList> tMoneyBeanList;
    private String sort_image;//饼状图与之相对应的分类图片地址
    private String sort_name;
    private String back_color;

    private MonthChartBean monthChartBean;

    private MonthChartAdapter adapter;

    private String setYear = DateUtils.getCurYear(FORMAT_Y);
    private String setMonth = DateUtils.getCurMonth(FORMAT_M);


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_menu_chart;
    }


    @Override
    protected void initEventAndData() {

        PieChartUtils.initPieChart(mChart);
        mChart.setOnChartValueSelectedListener(this);
        //设置中间透明圈的半径,值为所占饼图的百分比
        mChart.setTransparentCircleRadius(40);
        //设置圆盘是否转动，默认转动
        mChart.setRotationEnabled(true);
        //改变加载显示的颜色
        swipe.setColorSchemeColors(getResources().getColor(R.color.text_red), getResources().getColor(R.color.text_red));
        //设置向下拉多少出现刷新
        swipe.setDistanceToTriggerSync(200);
        //设置刷新出现的位置
        swipe.setProgressViewEndTarget(false, 200);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipe.setRefreshing(false);
                setChartData(Constants.currentUserId, setYear, setMonth);
            }
        });


        rvList.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new MonthChartAdapter(getActivity(), null);
        rvList.setAdapter(adapter);

        //请求当月数据
        setChartData(Constants.currentUserId, setYear, setMonth);
    }


    /**
     * 获取账单数据
     * @param userid
     * @param year
     * @param month
     */
    private void setChartData(final int userid, String year, String month) {
        if (userid==0){
            Toast.makeText(getContext(), "请先登陆", Toast.LENGTH_SHORT).show();
            return;
        }
        dataYear.setText(setYear + " 年");
        dataMonth.setText(setMonth);
        //请求某年某月数据
        OkHttpUtils.getInstance().get(Constants.BASE_URL + Constants.BILL_MONTH_CHART
                        + "/" + userid + "/" + year + "/" + month,
                null, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Gson gson = new Gson();
                        monthChartBean = gson.fromJson(response.body().string(), MonthChartBean.class);
                        //data不为空且status==100:处理成功！
                        if (monthChartBean!=null && monthChartBean.getStatus() == 100) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setReportData();
                                }
                            });
                        }
                    }
                });
    }

    /**
     * 报表数据
     */
    private void setReportData() {

        if (monthChartBean == null) {
            return;
        }

        float totalMoney;
        if (TYPE) {
            centerTitle.setText("总支出");
            centerImg.setImageResource(R.mipmap.tallybook_output);
            tMoneyBeanList = monthChartBean.getOutSortlist();
            totalMoney = Float.parseFloat(monthChartBean.getTotalOut());
        } else {
            centerTitle.setText("总收入");
            centerImg.setImageResource(R.mipmap.tallybook_input);
            tMoneyBeanList = monthChartBean.getInSortlist();
            totalMoney = Float.parseFloat(monthChartBean.getTotalIn());
        }

        tOutcome.setText(monthChartBean.getTotalOut());
        tIncome.setText(monthChartBean.getTotalIn());
        centerMoney.setText("" + totalMoney);

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (tMoneyBeanList != null && tMoneyBeanList.size() > 0) {
            layoutTypedata.setVisibility(View.VISIBLE);
            for (int i = 0; i < tMoneyBeanList.size(); i++) {
                float scale = Float.parseFloat(tMoneyBeanList.get(i).getMoney()) / totalMoney;
                float value = (scale < 0.06f) ? 0.06f : scale;
                entries.add(new PieEntry(value, PieChartUtils.getDrawable(tMoneyBeanList.get(i).getSort().getSortImg())));
                colors.add(Color.parseColor(tMoneyBeanList.get(i).getBack_color()));
            }
            setNoteData(0);
        } else {//无数据时的显示
            layoutTypedata.setVisibility(View.GONE);
            entries.add(new PieEntry(1f));
            colors.add(0xffAAAAAA);
        }

        PieChartUtils.setPieChartData(mChart, entries, colors);
    }

    /**
     * 点击饼状图上区域后相应的数据设置
     *
     * @param index
     */
    private void setNoteData(int index) {
        sort_image = tMoneyBeanList.get(index).getSort().getSortImg();
        sort_name = tMoneyBeanList.get(index).getSort().getSortName();
        back_color = tMoneyBeanList.get(index).getBack_color();
        if (TYPE) {
            money.setText("-" + tMoneyBeanList.get(index).getMoney());
        } else {
            money.setText("+" + tMoneyBeanList.get(index).getMoney());
        }
        title.setText(sort_name);
        rankTitle.setText(sort_name + "排行榜");
        circleBg.setImageDrawable(new ColorDrawable(Color.parseColor(back_color)));
        circleImg.setImageDrawable(PieChartUtils.getDrawable(tMoneyBeanList.get(index).getSort().getSortImg()));

        adapter.setSortName(sort_name);
        adapter.setmDatas(tMoneyBeanList.get(index).getList());
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onValueSelected(Entry e, Highlight h) {
        if (e == null)
            return;
        int entryIndex = (int) h.getX();
        PieChartUtils.setRotationAngle(mChart, entryIndex);
        setNoteData(entryIndex);
    }


    @Override
    public void onNothingSelected() {
        Log.i("PieChart", "nothing selected");
    }


    @OnClick({R.id.layout_center, R.id.layout_data, R.id.item_type, R.id.item_other})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.layout_center:

                TYPE = !TYPE;
                setReportData();
                break;
            case R.id.layout_data:
                //时间选择器
                new TimePickerView.Builder(getActivity(), new TimePickerView.OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {//选中事件回调
                        setYear = DateUtils.date2Str(date, "yyyy");
                        setMonth = DateUtils.date2Str(date, "MM");
                        setChartData(Constants.currentUserId, setYear, setMonth);
                    }
                })
                        .setRangDate(null, Calendar.getInstance())
                        .setType(new boolean[]{true, true, false, false, false, false})
                        .build()
                        .show();
                break;
            case R.id.item_type:
                break;
            case R.id.item_other:
                break;
        }
    }

}
