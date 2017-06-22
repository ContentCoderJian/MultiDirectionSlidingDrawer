package com.jian.mybus.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xutils.x;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.MarkerOptions.MarkerAnimateType;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.busline.BusLineResult;
import com.baidu.mapapi.search.busline.BusLineSearch;
import com.baidu.mapapi.search.busline.BusLineSearchOption;
import com.baidu.mapapi.search.busline.OnGetBusLineSearchResultListener;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.jian.mybus.MyApplication;
import com.jian.mybus.R;
import com.jian.mybus.adapter.BusDetailAdapter;
import com.jian.mybus.entity.Line;
import com.jian.mybus.presenter.IRoutePresenter;
import com.jian.mybus.presenter.impl.RoutePresenterImpl;
import com.jian.mybus.util.DialogUtil;
import com.jian.mybus.util.GlobalConsts;
import com.jian.mybus.util.LocationUtil;
import com.jian.mybus.util.LogUtil;
import com.jian.mybus.util.Tools;
import com.jian.mybus.util.overlayutil.OverlayManager;
import com.jian.mybus.view.IRouteView;
import com.jian.mybus.widget.MultiDirectionSlidingDrawer;
import com.jian.mybus.widget.MultiDirectionSlidingDrawer.OnDrawerCloseListener;
import com.jian.mybus.widget.MultiDirectionSlidingDrawer.OnDrawerOpenListener;
import com.jian.mybus.widget.ZoomControlsView;

@ContentView(R.layout.activity_bus_detail)
public class BusDetailActivity extends BaseActivity implements
		OnGetPoiSearchResultListener, OnGetBusLineSearchResultListener,
		OnClickListener, OnDrawerCloseListener, OnDrawerOpenListener,
		BaiduMap.OnMapClickListener, BDLocationListener, IRouteView {

	// 声明控件
	@ViewInject(R.id.iv_map_locate)
	private ImageView ivMapLocate;
	@ViewInject(R.id.zoomControlsView)
	private ZoomControlsView mzoomControlsView; // 缩放控件
	@ViewInject(R.id.bmapView)
	private MapView mMapView;
	@ViewInject(R.id.iv_busline_back)
	private ImageView ivBusLineBack;
	@ViewInject(R.id.iv_busline_function_menu)
	private ImageView ivBusLineFunctionMenu;
	@ViewInject(R.id.tv_bus_line)
	private TextView tvBusLine;
	@ViewInject(R.id.tv_bus_detail_route)
	private TextView tvBusDetailRoute;
	@ViewInject(R.id.tv_bus_detail_route_info)
	private TextView tvBusDetailRouteInfo;
	@ViewInject(R.id.tv_bus_detail_kaiwang)
	private TextView tvBusDetailKaiWang;
	@ViewInject(R.id.tv_bus_detail_fangxiang)
	private TextView tvBusDetailFangxiang;
	// @ViewInject(R.id.content)
	@ViewInject(R.id.lv_busdetail_route)
	private ListView lvBusDetailRoute;
	@ViewInject(R.id.rl_map_route)
	private RelativeLayout rlMapRoute;
	@ViewInject(R.id.drawer)
	private MultiDirectionSlidingDrawer mSlidingDrawer;
	@ViewInject(R.id.ll_more)
	private LinearLayout llMore;
	@ViewInject(R.id.rl_list)
	private RelativeLayout rlList;
	@ViewInject(R.id.rl_map)
	private RelativeLayout rlMap;
	@ViewInject(R.id.rl_more_refresh)
	private RelativeLayout rlRefresh;

	// 列表显示相关
	private IRoutePresenter presenter;
	private BusDetailAdapter adapter;
	private List<String> stats;
	private String line_name;
	private String name_dist;
	private String textColor;

	// 搜索相关
	private PoiSearch mSearch; // 搜索模块，也可去掉地图模块独立使用
	private BusLineResult route; // 保存驾车/步行路线数据的变量，供浏览节点时使用
	private BusLineSearch mBusLineSearch;
	private List<String> busLineIDList; // 公交列表信息
	private int busLineIndex = 0;

	// 地图显示相关
	private BaiduMap mBaiduMap;
	private MyBusLineOverlay overlay;// 公交路线绘制对象
	private Marker locationOverlay; // 定位覆盖物标记
	private LocationClient mLocationClient; // 定位
	private boolean isFirstLoc = true;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case GlobalConsts.MESSAGE_STOP_LOCATION:
				mLocationClient.stop();
				break;
			case GlobalConsts.MESSAGE_SEARCH_OPPOSITE:
				searchNextBusline();
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// 设置个性地图
		MapView.setMapCustomEnable(true);
		x.view().inject(BusDetailActivity.this);
		setMapCustomFile(this);
		super.onCreate(savedInstanceState);
		// 初始化presenter
		presenter = new RoutePresenterImpl(this);

		// 获取用于激活当前activity的Intent对象并获取数据给控件赋值
		getIntentAndSetViews();

		// 默认显示列表
		mSlidingDrawer.open();

		// 初始化地图
		initBaiduMaps();

		// 配置定位参数
		initLocation();

		// 默认开启定位
		mLocationClient.start();

		// 设置监听器
		setListeners();

	}

	/**
	 * 查询公交
	 */
	private void searchBusRoute() {
		// 查询公交线路
		searchRouteProcess();
		searchNextBusline();
		handler.sendEmptyMessageDelayed(GlobalConsts.MESSAGE_SEARCH_OPPOSITE,
				100);
	}

	/**
	 * 配置定位参数
	 */
	private void initLocation() {
		// 让百度地图框架中接口BdlocationListener指向我们的实现类
		mLocationClient = new LocationClient(BusDetailActivity.this);
		mLocationClient.registerLocationListener(BusDetailActivity.this);

		// 设置定位参数
		LocationClientOption clientOption = new LocationClientOption();
		clientOption.setOpenGps(true);

		// 设置定位模式
		clientOption.setLocationMode(LocationMode.Hight_Accuracy);
		// 设置坐标类型 bd0911(百度坐标类型)
		clientOption.setCoorType("bd0911");
		// 设置发起定位请求的间隔时间(最少1000)
		// clientOption.setScanSpan(5000);
		// 返回的定位结果包含地址信息
		clientOption.setIsNeedAddress(true);
		clientOption.setIsNeedLocationDescribe(true);
		// 设置返回结果包含手机的方向
		clientOption.setNeedDeviceDirect(true);
		mLocationClient.setLocOption(clientOption);
	}

	/**
	 * 获取数据并给控件赋值
	 * 
	 */
	private void getIntentAndSetViews() {
		Intent intent = getIntent();
		line_name = intent.getStringExtra(GlobalConsts.ROUTE_LINE_NAME);
		name_dist = intent.getStringExtra(GlobalConsts.NAME_DIST); // 当前站点及距离
		textColor = "#2980B9"; // 默认字体蓝色
		String colorTemp = intent.getStringExtra(GlobalConsts.TEXT_COLOR);
		if (!"".equals(colorTemp)) { // 若是从nearbyFragment点开的,根据传递的颜色设置,否则使用默认蓝色
			textColor = colorTemp;
		}

		// 设置字体颜色
		tvBusDetailRoute.setTextColor(Color.parseColor(textColor));
		tvBusDetailRouteInfo.setTextColor(Color.parseColor(textColor));
		tvBusDetailKaiWang.setTextColor(Color.parseColor(textColor));
		tvBusDetailFangxiang.setTextColor(Color.parseColor(textColor));

		// 显示加载对话框
		mLoadingDialog.show();
		// 通过传递过来的line_name查询并加载线路信息
		presenter.loadBusRouteList(line_name, true);
	}

	/**
	 * 根据经纬度初始化百度地图以及其他模块
	 * 
	 */
	private void initBaiduMaps() {
		mBaiduMap = mMapView.getMap();
		mMapView.removeViewAt(1); // 去掉百度logo
		mMapView.showZoomControls(false); // 去掉百度默认比例尺
		mMapView.showScaleControl(false); // 去掉百度地图自带的缩放按钮
		mzoomControlsView.setMapView(mMapView);

		busLineIDList = new ArrayList<String>();
		mSearch = PoiSearch.newInstance();
		mBusLineSearch = BusLineSearch.newInstance();
	}

	/**
	 * 设置监听
	 */
	private void setListeners() {
		// 点击监听
		ivMapLocate.setOnClickListener(this);
		ivBusLineBack.setOnClickListener(this);
		ivBusLineFunctionMenu.setOnClickListener(this);
		rlMap.setOnClickListener(this);
		rlList.setOnClickListener(this);
		rlRefresh.setOnClickListener(this);
		mSlidingDrawer.setOnDrawerCloseListener(this);
		mSlidingDrawer.setOnDrawerOpenListener(this);

		mBaiduMap.setOnMapClickListener(this);
		mSearch.setOnGetPoiSearchResultListener(this);
		mBusLineSearch.setOnGetBusLineSearchResultListener(this);
		overlay = new MyBusLineOverlay(mBaiduMap);
		mBaiduMap.setOnMarkerClickListener(overlay);
	}

	/**
	 * 发起检索
	 * 
	 * @param v
	 */
	public void searchRouteProcess() {
		busLineIDList.clear();
		busLineIndex = 0;
		String keyword = null;
		if (line_name.contains("[")) { // 最后含[]括号
			keyword = line_name.substring(0, line_name.indexOf("["));
		} else { // 最后是()
			keyword = line_name.substring(0, line_name.indexOf("("));
		}
		LogUtil.i("mybus", "keyword=" + keyword);
		// 发起poi检索，从得到所有poi中找到公交线路类型的poi，再使用该poi的uid进行公交详情搜索
		mSearch.searchInCity((new PoiCitySearchOption()).city(
				GlobalConsts.CITY_NAME).keyword(keyword));
	}

	public void searchNextBusline() {
		if (busLineIndex >= busLineIDList.size()) {
			busLineIndex = 0;
		}
		if (busLineIndex >= 0 && busLineIndex < busLineIDList.size()
				&& busLineIDList.size() > 0) {
			mBusLineSearch.searchBusLine((new BusLineSearchOption()
					.city((GlobalConsts.CITY_NAME)).uid(busLineIDList
					.get(busLineIndex))));

			busLineIndex++;
		}

	}

	@Override
	public void onGetBusLineResult(BusLineResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Tools.showToast(getApplicationContext(), "此线路暂不支持地图显示");
			return;
		}
		mBaiduMap.clear();
		route = result;
		overlay.removeFromMap();
		overlay.setData(result);
		overlay.addToMap();
		overlay.zoomToSpan();
		LogUtil.i("mybus",
				"baidumap -- getBusLineResult " + result.getBusLineName());
	}

	@Override
	public void onGetPoiResult(PoiResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Tools.showToast(getApplicationContext(), "此线路暂不支持地图显示");
			return;
		}
		// 遍历所有poi，找到类型为公交线路的poi
		// busLineIDList.clear();
		for (PoiInfo poi : result.getAllPoi()) {
			if (poi.type == PoiInfo.POITYPE.BUS_LINE
					|| poi.type == PoiInfo.POITYPE.SUBWAY_LINE) {
				busLineIDList.add(poi.uid);
			}
		}
		searchNextBusline();
		route = null;
	}

	@Override
	public void onMapClick(LatLng result) {
		mBaiduMap.hideInfoWindow();
	}

	@Override
	public boolean onMapPoiClick(MapPoi result) {
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume();
	}

	@Override
	protected void onDestroy() {
		// 关闭定位服务
		mLocationClient.unRegisterLocationListener(this);

		mSearch.destroy();
		mBusLineSearch.destroy();
		MapView.setMapCustomEnable(false);
		super.onDestroy();
	}

	// 设置个性化地图config文件路径
	private void setMapCustomFile(Context context) {
		FileOutputStream out = null;
		InputStream inputStream = null;
		String moduleName = null;
		try {
			inputStream = context.getAssets().open(
					"customConfigdir/custom_config.txt");
			byte[] b = new byte[inputStream.available()];
			inputStream.read(b);

			moduleName = context.getFilesDir().getAbsolutePath();
			File f = new File(moduleName + "/" + "custom_config.txt");
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
			out = new FileOutputStream(f);
			out.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		MapView.setCustomMapStylePath(moduleName + "/custom_config.txt");

	}

	@Override
	public void onReceiveLocation(BDLocation location) {
		// map view 销毁后不在处理新接收的位置
		if (location == null || mMapView == null) {
			LogUtil.d("mybus", "未定位显示");
			return;
		}
		if (locationOverlay != null) { // 若locationOverlay不为null,清除
			locationOverlay.remove();
		}
		LogUtil.i("mybus", "百度定位: 经度: " + location.getLongitude() + ", 纬度:"
				+ location.getLatitude());
		// 移动地图
		LatLng currentLocation = new LatLng(location.getLatitude(),
				location.getLongitude());
		if (isFirstLoc) {
			isFirstLoc = false;
			MapStatus.Builder builder = new MapStatus.Builder();
			builder.target(currentLocation).zoom(16.0f);
			mBaiduMap.animateMapStatus(MapStatusUpdateFactory
					.newMapStatus(builder.build()));
		} else {
			MapStatus.Builder builder = new MapStatus.Builder();
			builder.target(currentLocation);
			mBaiduMap.animateMapStatus(MapStatusUpdateFactory
					.newMapStatus(builder.build()));
			// 显示图片
			MarkerOptions options = new MarkerOptions();
			BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory
					.fromResource(R.drawable.location);
			options.position(currentLocation).icon(bitmapDescriptor)
					.animateType(MarkerAnimateType.drop);
			// 在地图上添加Marker，并显示
			locationOverlay = (Marker) mBaiduMap.addOverlay(options);
			if (location.getAddrStr() != null) {
				locationOverlay.setTitle(location.getAddrStr());
			} else {
				locationOverlay.setTitle(location.getLocationDescribe());
			}
		}
		// 定位完毕后发消息,停止定位
		handler.sendEmptyMessage(GlobalConsts.MESSAGE_STOP_LOCATION);
	}

	@Override
	public void showEmptyInfo(String error) {
		// 隐藏加载对话框
		if (mLoadingDialog.isShowing()) {
			mLoadingDialog.dismiss();
		}
		// toast提示error信息
		if (error != null) {
			LogUtil.i("mybus", "出错信息: " + error);
			Tools.showToast(getApplicationContext(), "网络请求失败, 请点击刷新重试");
		}
	}

	@Override
	public void updateBusRouteList(List<Line> lines) {
		// 隐藏加载对话框
		if (mLoadingDialog.isShowing()) {
			mLoadingDialog.dismiss();
		}
		String name = lines.get(0).getName();
		tvBusDetailRoute.setText(name.substring(name.indexOf("-") + 1,
				name.lastIndexOf(")")));
		String info = lines.get(0).getInfo();
		// 判断服务器中的info是否不为空
		if ("".equals(info) || info == null) {
			tvBusDetailRouteInfo.setText("该线路暂无详细信息");
		} else {
			tvBusDetailRouteInfo.setText(info);
		}

		String[] statsStr = lines.get(0).getStats().split(";");
		stats = Arrays.asList(statsStr);
		tvBusLine.setText(line_name.substring(0, line_name.indexOf("(")) + " ("
				+ stats.size() + "站)");
		adapter = new BusDetailAdapter(this, stats, name_dist, textColor);
		lvBusDetailRoute.setAdapter(adapter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_map_locate:
			// 检查定位服务状态
			if (MyApplication.GPSIsClose) {
				LocationUtil.toggleGPS(BusDetailActivity.this);
			} else {
				mLocationClient.start();
			}
			break;

		case R.id.iv_busline_back:
			finish();
			break;
		case R.id.iv_busline_function_menu:
			if (llMore.getVisibility() != View.VISIBLE) { // 未显示
				llMore.setVisibility(View.VISIBLE);
			} else {
				llMore.setVisibility(View.GONE);
			}
			break;
		case R.id.rl_list:
			rlList.setVisibility(View.GONE);
			rlMap.setVisibility(View.VISIBLE);
			// 显示线路列
			if (!mSlidingDrawer.isOpened()) {
				mSlidingDrawer.animateOpen();
			}
			llMore.setVisibility(View.GONE);
			break;
		case R.id.rl_map:
			rlMap.setVisibility(View.GONE);
			rlList.setVisibility(View.VISIBLE);
			// 显示地图
			if (mSlidingDrawer.isOpened()) {
				mSlidingDrawer.animateClose();
			}
			llMore.setVisibility(View.GONE);
			break;
		case R.id.rl_more_refresh:
			// 显示加载对话框
			mLoadingDialog.show();
			// 重新获取数据
			presenter.loadBusRouteList(line_name, true);
			llMore.setVisibility(View.GONE);
			// 10秒后强制关闭对话框
			DialogUtil.dismissDialog(mLoadingDialog, 10000);
			break;
		}
	}

	/**
	 * 列表抽屉关闭监听
	 */
	@Override
	public void onDrawerClosed() {
		// 查询公交
		searchBusRoute();

		// 重新显示地图上的按钮
		ivMapLocate.setVisibility(View.VISIBLE);
		mzoomControlsView.setVisibility(View.VISIBLE);

	}

	/**
	 * 列表抽屉拉开监听
	 */
	@Override
	public void onDrawerOpened() {
		// 隐藏地图上的按钮
		ivMapLocate.setVisibility(View.GONE);
		mzoomControlsView.setVisibility(View.GONE);
	}

	@Override
	public void onGetPoiIndoorResult(PoiIndoorResult arg0) {

	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) {

	}

	/**
	 * 为了站点点击弹出气泡继承OverlayManager
	 * 
	 * @author Jian
	 * 
	 */
	private class MyBusLineOverlay extends OverlayManager {
		private BusLineResult mBusLineResult = null;

		public MyBusLineOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}

		/**
		 * 设置公交线数据
		 * 
		 * @param result
		 *            公交线路结果数据
		 */
		public void setData(BusLineResult result) {
			this.mBusLineResult = result;
		}

		@Override
		public final List<OverlayOptions> getOverlayOptions() {

			if (mBusLineResult == null || mBusLineResult.getStations() == null) {
				return null;
			}
			List<OverlayOptions> overlayOptionses = new ArrayList<OverlayOptions>();
			// CharSequence[] keyword = { ".", "(", ")", "（", " （", "）", ",",
			// "·" };
			// // statsStart
			// String statsStart = stats.get(0);
			// if (statsStart.contains("[")) {
			// String temp = statsStart.substring(statsStart.indexOf("["),
			// statsStart.indexOf("]") + 1);
			// statsStart = statsStart.replace(temp, "");
			// }
			// if (statsStart.contains("(")) {
			// statsStart = statsStart.substring(0, statsStart.indexOf("("));
			// }
			// if ((statsStart.substring(statsStart.length() - 2)).equals("总站"))
			// {
			// statsStart = statsStart.replace("总站", "");
			// }
			// if ((statsStart.substring(statsStart.length() - 1)).equals("站"))
			// {
			// statsStart = statsStart.replace("站", "");
			// }
			// LogUtil.v("mybus", "statsStart: " + statsStart);
			//
			// // statsEnd
			// String statsEnd = stats.get(stats.size() - 1);
			// if (statsEnd.contains("[")) {
			// String temp = statsEnd.substring(statsEnd.indexOf("["),
			// statsEnd.indexOf("]") + 1);
			// statsEnd = statsEnd.replace(temp, "");
			// }
			// if (statsEnd.contains("(")) {
			// statsEnd = statsEnd.substring(0, statsEnd.indexOf("("));
			// }
			// if ((statsEnd.substring(statsEnd.length() - 2)).equals("总站")) {
			// statsEnd = statsEnd.replace("总站", "");
			// }
			// if ((statsEnd.substring(statsEnd.length() - 1)).equals("站")) {
			// statsEnd = statsEnd.replace("站", "");
			// }
			// LogUtil.w("mybus", "statsEnd: " + statsEnd);
			//
			// // route
			// String route = null;
			for (BusLineResult.BusStation station : mBusLineResult
					.getStations()) {
				// route = Tools.replaceKey(station.getTitle(), keyword);
				// if (route.contains("(")) {
				// route = route.replace("(", "");
				// }
				// if (route.contains(")")) {
				// route = route.replace(")", "");
				// }
				// LogUtil.w("mybus", "BusDetailActivity route: " + route);
				// if (statsStart.equals(route) || route.indexOf(statsStart) !=
				// -1) {
				// overlayOptionses.add(new MarkerOptions()
				// .position(station.getLocation())
				// .zIndex(10)
				// .anchor(0.5f, 0.5f)
				// .icon(BitmapDescriptorFactory
				// .fromResource(R.drawable.start)));
				// } else if (statsEnd.equals(route)
				// || route.indexOf(statsEnd) != -1) {
				// overlayOptionses.add(new MarkerOptions()
				// .position(station.getLocation())
				// .zIndex(10)
				// .anchor(0.5f, 0.5f)
				// .icon(BitmapDescriptorFactory
				// .fromResource(R.drawable.end)));
				// } else {
				if ("#2980B9".equals(textColor)) {
					overlayOptionses.add(new MarkerOptions()
							.position(station.getLocation())
							.zIndex(10)
							.anchor(0.5f, 0.5f)
							.animateType(MarkerAnimateType.drop)
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.purplestop)));
				} else {
					overlayOptionses.add(new MarkerOptions()
							.position(station.getLocation())
							.zIndex(10)
							.anchor(0.5f, 0.5f)
							.animateType(MarkerAnimateType.drop)
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.bluestop)));
				}
			}
			// }

			List<LatLng> points = new ArrayList<LatLng>();
			for (BusLineResult.BusStep step : mBusLineResult.getSteps()) {
				if (step.getWayPoints() != null) {
					points.addAll(step.getWayPoints());
				}
			}
			if (points.size() > 0) {
				overlayOptionses.add(new PolylineOptions().width(10)
						.color(Color.argb(178, 0, 78, 255)).zIndex(0)
						.points(points));
			}
			return overlayOptionses;
		}

		public boolean onBusStationClick(int index) {
			if (mBusLineResult.getStations() != null
					&& mBusLineResult.getStations().get(index) != null) {

				LogUtil.i(
						"mybus",
						"BusLineOverlay onBusStationClick"
								+ route.getLineDirection());

				TextView popupText = new TextView(BusDetailActivity.this);
				popupText.setBackgroundResource(R.drawable.map_info_bubble);
				popupText.setTextSize(16.0f);
				popupText.setTextColor(Color.parseColor("#000000"));
				popupText.setClickable(true);
				// 移动到指定索引的坐标
				mBaiduMap.setMapStatus(MapStatusUpdateFactory
						.newLatLng(mBusLineResult.getStations().get(index)
								.getLocation()));
				// 弹出泡泡
				popupText.setText(mBusLineResult.getStations().get(index)
						.getTitle());
				mBaiduMap.showInfoWindow(new InfoWindow(popupText,
						mBusLineResult.getStations().get(index).getLocation(),
						10));
			}
			return true;
		}

		@Override
		public final boolean onMarkerClick(Marker marker) {
			if (mOverlayList != null && mOverlayList.contains(marker)) {
				return onBusStationClick(mOverlayList.indexOf(marker));
			} else if (locationOverlay != null) {
				TextView popupText = new TextView(BusDetailActivity.this);
				popupText.setBackgroundResource(R.drawable.map_info_bubble);
				popupText.setTextSize(16.0f);
				popupText.setTextColor(Color.parseColor("#000000"));
				popupText.setClickable(true);
				popupText.setText(marker.getTitle());
				// 创建InfoWindow
				InfoWindow mInfoWindow = new InfoWindow(popupText,
						marker.getPosition(), -25);
				mBaiduMap.showInfoWindow(mInfoWindow); // 显示气泡
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean onPolylineClick(Polyline arg0) {
			return false;
		}
	}

}
