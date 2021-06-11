package com.ssusp.canbus;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apmem.tools.layouts.FlowLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PathFindActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final static String TAG = "PathFindActivity";

    private RelativeLayout rl_container;
    private RelativeLayout rl_another_view; //다른 경로
    private RelativeLayout rl_route_view; //추천 경로
    private LinearLayout ll_detail_course_container;
    private LinearLayout ll_traffic_detail_route_container; //상세 경로
    private LinearLayout ll_flow_container;
    private FlowLayout fl_route;
    private FlowLayout fl_another_route;

    private Spinner spinner = null;  //드롭다운
    private String[] spinnerArr = null;
    private String selected_spinner = null;

    private EditText EditText_arrival = null; //출발지 입력
    private EditText EditText_destination = null; //도착지 입력
    private Button arrVoice;
    private Button destVoice;
    private Button pathFind; //경로 검색
    private ImageButton ib_detail_remove;

    private GoogleMap mMap; // 구글 지도
    private Marker start_m; // 시작 마커
    private Marker end_m; // 도착 마커
    private Marker[][] marker_arr; // 중간 마커 배열
    private LatLng End_location; // 도착 위치 표시
    private Drawable img = null;


    // Directions API 관련 변수
    private static final String API_KEY = "AIzaSyCP-aqDnF1JpAjpMYqYJXg8PWdJTumBLSo";
    private String str_url = null; // EditText의 값과 원래의 URL을 합쳐 검색 URL을 만들어 저장
    private String option = null;
    private String[] full_time;
    private String[] hours;
    private String[] min;
    private String arrival_lat = null;
    private String arrival_lng = null;
    private String[][] goingS_lat;
    private String[][] goingS_lng;
    private String[][] goingE_lat;
    private String[][] goingE_lng;
    private String destination_lat = null;
    private String destination_lng = null;
    private String[][] TransitName;
    private String[][] getPolyline;
    private String[][] getInstructions;
    private String[][] step = null;
    private String getOverview = null;
    private String REQUEST_ARR = null;
    private String REQUEST_DEST = null;

    private int r_list_len = 0;
    private int[] list_len = null;
    private int fl_count = 0;
    private int R_fl_count = 0;

    private void initAllComponent() {

        EditText_arrival = findViewById(R.id.EditText_arrival);
        EditText_destination = findViewById(R.id.EditText_destination);
        pathFind = findViewById(R.id.pathFind);
        ib_detail_remove = findViewById(R.id.ib_detail_remove);
        rl_container = findViewById(R.id.rl_container);
        fl_route = findViewById(R.id.fl_route);
        rl_route_view = findViewById(R.id.rl_route_view);
        rl_another_view = findViewById(R.id.rl_another_view);
        ll_flow_container = findViewById(R.id.ll_flow_container);
        ll_detail_course_container = findViewById(R.id.ll_detail_course_container);
        ll_traffic_detail_route_container = findViewById(R.id.ll_traffic_detail_route_container);
        arrVoice = findViewById(R.id.arrVoice);
        destVoice = findViewById(R.id.destVoice);


    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_find);


        initAllComponent();

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();

            if (extras != null) {
                REQUEST_ARR = extras.getString("from");
                REQUEST_DEST = extras.getString("to");

                EditText_arrival.setText(REQUEST_ARR);
                EditText_destination.setText(REQUEST_DEST);
            }
        }

        EditText_arrival.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int KeyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && KeyCode == KeyEvent.KEYCODE_ENTER) {
                    EditText enter_action = findViewById(R.id.EditText_destination);

                    enter_action.requestFocus();
                    return true;
                }
                return false;
            }
        });

        EditText_destination.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int KeyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && KeyCode == KeyEvent.KEYCODE_ENTER) {
                    pathFind.performClick();
                    return true;
                }
                return false;
            }
        });

        pathFind.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                mMap.clear(); // 맵을 clear

                r_list_len = 0;
                list_len = null; // 다시 값 초기화

                rl_container.setVisibility(VISIBLE);

                String arrival = EditText_arrival.getText().toString();
                String destination = EditText_destination.getText().toString();

                if (!arrival.isEmpty() && !destination.isEmpty()) {
                    directions(arrival, destination);

                    if (getOverview != null) {
                        ArrayList<LatLng> entire_path = decodePolyPoints(getOverview);

                        for (int i = 0; i < entire_path.size(); i++) {
                            if (i == 0) {
                                mMap.addMarker(new MarkerOptions().position(entire_path.get(i)).title("출발"));
                            } else if (i >= entire_path.size() - 1) {
                                mMap.addMarker(new MarkerOptions().position(entire_path.get(i)).title("도착"));
                            }
                        }

                        Polyline line = null;
                        if (line == null) {
                            line = mMap.addPolyline(new PolylineOptions()
                                    .color(Color.rgb(58, 122, 255))
                                    .geodesic(true)
                                    .addAll(entire_path));
                        } else {
                            line.remove();
                            line = mMap.addPolyline(new PolylineOptions()
                                    .color(Color.rgb(58, 122, 255))
                                    .geodesic(true)
                                    .addAll(entire_path));
                        }

                        LatLng arrival_path = entire_path.get(entire_path.size() - 1);
                        onMapReady(mMap);
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(arrival_path.latitude, arrival_path.longitude)));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                    }

                } else {
                    if (!arrival.isEmpty() && destination.isEmpty())
                        Toast.makeText(getApplicationContext(), "도착지를 작성해주세요.", Toast.LENGTH_SHORT).show();
                    else if (arrival.isEmpty() && !destination.isEmpty())
                        Toast.makeText(getApplicationContext(), "출발지를 작성해주세요.", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), "출발지와 도착지를 작성해주세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        arrVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(com.ssusp.canbus.PathFindActivity.this, VoiceSearchActivity.class);
                startActivity(intent);
            }
        });

        destVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(com.ssusp.canbus.PathFindActivity.this, VoiceSearchActivity.class);
                startActivity(intent);
            }
        });


        ib_detail_remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ll_detail_course_container.setVisibility(GONE);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

    }


    public void directions(String arrival, String destination) {

        str_url = "https://maps.googleapis.com/maps/api/directions/json?"+"origin="+arrival+"&destination="+destination+"&mode=transit"+"&departure_time=now"+"&alternatives=true&language=Korean&key="+API_KEY;

        System.out.println(str_url);

        String resultText;

        try {

            if (fl_count >= 1) {
                ll_flow_container.removeAllViews();
                fl_count = 0;
            }
            if (R_fl_count >= 1) {
                fl_route.removeAllViews();
                R_fl_count = 0;
            }

            resultText = new Task().execute().get(); // URL에 있는 내용을 받아옴

            JSONObject jsonObject = new JSONObject(resultText);
            boolean routecheck = jsonObject.isNull("routes");
            if (routecheck == true) {
                Toast.makeText(getApplicationContext(), "경로가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                System.out.println("경로 x");
            } else {
                String routes = jsonObject.getString("routes");

                if (routes.isEmpty()) { // 경로가 존재하지 않는다면
                    Toast.makeText(getApplicationContext(), "경로가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    System.out.println("경로 x");
                }
                JSONArray routesArray = new JSONArray(routes);
                r_list_len = routesArray.length();
                if(r_list_len <= 0) {
                    Toast.makeText(getApplicationContext(), "경로가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    System.out.println("경로 x");
                } else if (r_list_len > 0) {

                    list_len = new int[r_list_len]; // route의 개수만큼 배열 동적 생성

                    full_time = new String[r_list_len];
                    hours = new String[r_list_len];
                    min = new String[r_list_len];

                    goingS_lat = new String[r_list_len][20]; // route의 개수만큼 그리고 그 안에 자잘한 route들을 최대 20으로 배열을 생성
                    goingS_lng = new String[r_list_len][20];
                    goingE_lat = new String[r_list_len][20];
                    goingE_lng = new String[r_list_len][20];
                    getPolyline = new String[r_list_len][20];
                    getInstructions = new String[r_list_len][];
                    TransitName = new String[r_list_len][20];
                    step = new String[r_list_len][];

                    marker_arr = new Marker[2][20];

                    JSONObject preferredObject = routesArray.getJSONObject(0);
                    String singleRoute = preferredObject.getString("overview_polyline");
                    JSONObject pointsObject = new JSONObject(singleRoute);
                    String points = pointsObject.getString("points");
                    getOverview = points;

                    for (int j = 0; j < routesArray.length(); j++) { // 배열들 생성 및 초기화

                        JSONObject subJsonObject = routesArray.getJSONObject(j);

                        String legs = subJsonObject.getString("legs");
                        JSONArray LegArray = new JSONArray(legs);
                        JSONObject legJsonObject = LegArray.getJSONObject(0);

                        String steps = legJsonObject.getString("steps");
                        JSONArray stepsArray = new JSONArray(steps);

                        list_len[j] = stepsArray.length(); // j번째 route에 step이 몇개인지 저장

                        for (int i = 0; i < list_len[j]; i++) {

                            goingS_lat[j][i] = null;
                            goingS_lng[j][i] = null;
                            goingE_lat[j][i] = null;
                            goingE_lng[j][i] = null;
                            getPolyline[j][i] = null;
                            TransitName[j][i] = null;
                            marker_arr[0][i] = null;
                            marker_arr[1][i] = null;
                        }

                    }

                    for (int j = 0; j < routesArray.length(); j++) {

                        JSONObject subJsonObject = routesArray.getJSONObject(j);

                        String legs = subJsonObject.getString("legs");
                        JSONArray LegArray = new JSONArray(legs);
                        JSONObject legJsonObject = LegArray.getJSONObject(0);

                        String leg_duration = legJsonObject.getString("duration");
                        JSONObject legdurObject = new JSONObject(leg_duration);
                        String amountDuration = legdurObject.getString("text");
                        String[] set_time = amountDuration.split(" ").clone();
                        for (int k = 0; k < set_time.length; k++) {
                            if (set_time[k].contains("시간") || set_time[k].contains("hours")) {
                                hours[j] = set_time[k];
                            } else if (set_time[k].contains("분") || set_time[k].contains("min")) {
                                min[j] = set_time[k];
                            }
                        }

                        if (hours[j] == null || hours[j].isEmpty()) {
                            full_time[j] = min[j];
                        } else {
                            full_time[j] = hours[j] + " " + min[j];
                        }

                        if (j > 0) {
                            TextView time = new TextView(this);
                            time.setText(full_time[j]);

                            time.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) time.getLayoutParams();
                            params.gravity = Gravity.RIGHT;
                            time.setLayoutParams(params);
                            time.setTextSize(20);

                            time.setTextColor(Color.BLACK);
                            time.setBackgroundColor(Color.WHITE);
                            time.setPadding(5, 0, 5, 0);

                            ll_flow_container.addView(time);
                        }

                        fl_another_route = new FlowLayout(PathFindActivity.this);
                        FlowLayout.LayoutParams param = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        // param.bottomMargin = 50;
                        fl_another_route.setLayoutParams(param);

                        fl_another_route.setOrientation(FlowLayout.HORIZONTAL);
                        fl_another_route.setBackgroundColor(Color.WHITE);
                        ll_flow_container.addView(fl_another_route);

                        String steps = legJsonObject.getString("steps");
                        JSONArray stepsArray = new JSONArray(steps);

                        String[] getTravelMode = new String[list_len[j]]; // j번째 route의 step 수만큼 배열 동적 생성
                        String isTrain = null;
                        String isSubway = null;
                        String[] getDuration = new String[list_len[j]];
                        String[] destination_name = new String[list_len[j]];
                        String[] arrival_name = new String[list_len[j]];
                        String[] getTransit = new String[list_len[j]];
                        getInstructions[j] = new String[list_len[j]];
                        step[j] = new String[list_len[j]];

                        for (int i = 0; i < list_len[j]; i++) {

                            JSONObject stepsObject = stepsArray.getJSONObject(i);
                            getTravelMode[i] = stepsObject.getString("travel_mode");

                            getInstructions[j][i] = stepsObject.getString("html_instructions");
                            Log.d(TAG, "instructions : " + getInstructions[j][i]);
                            isTrain = getInstructions[j][i].split(" ")[0];
                            Log.d(TAG, "isTrain : " + isTrain);

                            String end_location = stepsObject.getString("end_location");
                            JSONObject endJsonObject = new JSONObject(end_location);
                            if (i >= list_len[j] - 1) {
                                destination_lat = endJsonObject.getString("lat");
                                destination_lng = endJsonObject.getString("lng");

                                Double End_lat = Double.parseDouble(destination_lat);
                                Double End_lng = Double.parseDouble(destination_lng);
                                End_location = new LatLng(End_lat, End_lng);
                            } else {
                                goingE_lat[j][i] = endJsonObject.getString("lat");
                                goingE_lng[j][i] = endJsonObject.getString("lng");
                            }

                            String start_location = stepsObject.getString("start_location");
                            JSONObject startJsonObject = new JSONObject(start_location);
                            if (i == 0) {
                                arrival_lat = startJsonObject.getString("lat");
                                arrival_lng = startJsonObject.getString("lng");
                            } else {
                                goingS_lat[j][i] = startJsonObject.getString("lat");
                                goingS_lng[j][i] = startJsonObject.getString("lng");
                            }

                            String polyline = stepsObject.getString("polyline");
                            JSONObject polyJsonObject = new JSONObject(polyline);
                            getPolyline[j][i] = polyJsonObject.getString("points"); // 인코딩 된 포인트를 얻어옴

                            String duration = stepsObject.getString("duration");
                            JSONObject durJsonObject = new JSONObject(duration);
                            String tempDuration = durJsonObject.getString("text");
                            getDuration[i] = tempDuration.split(" ")[0];

                            if (getTravelMode[i].equals("TRANSIT")) {

                                String transit_details = stepsObject.getString("transit_details");
                                JSONObject transitObject = new JSONObject(transit_details);

                                String arrival_stop = transitObject.getString("arrival_stop");
                                JSONObject arrivalObject = new JSONObject(arrival_stop);
                                destination_name[i] = arrivalObject.getString("name");

                                String depart_stop = transitObject.getString("departure_stop");
                                JSONObject departObject = new JSONObject(depart_stop);
                                arrival_name[i] = departObject.getString("name");

                                String line = transitObject.getString("line");
                                JSONObject lineObject = new JSONObject(line);
                                if (isTrain.equals("Train")) {
                                    getTransit[i] = lineObject.getString("name");
                                }
                                else if (isTrain.equals("Subway")) {
                                    getTransit[i] = lineObject.getString("short_name");
                                    if (getTransit[i].equals("1")) {
                                        getTransit[i] += "호선";
                                    }
                                }
                                else if (isTrain.equals("Bus")) {
                                    if(!lineObject.isNull("short_name")) {
                                        getTransit[i] = lineObject.getString("short_name");
                                    }
                                    else getTransit[i] = lineObject.getString("name");
                                }

                                TransitName[j][i] = getTransit[i];

                            }

                            if (i < list_len[j] - 1) {
                                if (getTravelMode[i].equals("WALKING")) {
                                    step[j][i] = "도보 (" + getDuration[i] + "분) " + " > ";

                                } else if (getTravelMode[i].equals("TRANSIT")) {
                                    step[j][i] = getTransit[i] + " (" + getDuration[i] + "분) " + " > ";
                                }
                            } else {
                                if (getTravelMode[i].equals("WALKING")) {
                                    step[j][i] = "도보 ("+ getDuration[i] + "분) ";

                                } else if (getTravelMode[i].equals("TRANSIT")) {
                                    step[j][i] = getTransit[i] + " (" + getDuration[i] + "분) " ;
                                }
                            }

                            Log.d(TAG, "step : " + step[j][i]);

                            Resources res = getResources();

                            switch (isTrain) {
                                case "Train":
                                case "Subway":
                                    img = ResourcesCompat.getDrawable(res, R.drawable.subway, null);
                                    break;
                                case "Bus":
                                    img = ResourcesCompat.getDrawable(res, R.drawable.bus, null);
                                    break;
                                default:
                                    img = ResourcesCompat.getDrawable(res, R.drawable.walk, null);
                            }

                            method_view(full_time[j], img, j, i);
                        }
                    }
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

    }

    //over_view 폴리라인 포인트 디코드 필요
    public static ArrayList<LatLng> decodePolyPoints(String encodedPath) {
        int len = encodedPath.length();

        final ArrayList<LatLng> path = new ArrayList<LatLng>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new LatLng(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }

    //동적 텍스트뷰 생성 함수
    public void method_view(String t, Drawable img, int j, int i) {

        TextView ith_route = null;
        if (j == 0) { // j가 0이라면 추천 경로

            ith_route = new TextView(this);
            ith_route.setText(step[j][i]);
            ith_route.setTextSize(22);

            ith_route.setTextColor(Color.BLACK);

            int h = 130;
            int w = 130;
            img.setBounds(0, 0, w, h);
            ith_route.setCompoundDrawables(img, null, null, null);

            ith_route.setGravity(Gravity.CENTER_VERTICAL);
            ith_route.setTextColor(Color.BLACK);

            TextView time = findViewById(R.id.during_time);
            time.setText(t);

            R_fl_count += 1;
            fl_route.addView(ith_route);
            fl_route.setPadding(15, 20, 15, 20);
            rl_route_view.setVisibility(VISIBLE);

        } else if (j > 0) { // j가 0보다 크다면 다른 경로이므로

            ith_route = new TextView(this);
            ith_route.setText(step[j][i]);
            ith_route.setTextSize(22);
            ith_route.setTextColor(Color.BLACK);


            int h = 130;
            int w = 130;
            img.setBounds(0, 0, w, h);
            ith_route.setCompoundDrawables(img, null, null, null);

            ith_route.setGravity(Gravity.CENTER_VERTICAL);

            fl_count += 1;
            fl_another_route.addView(ith_route);
            rl_another_view.setVisibility(VISIBLE);
            rl_another_view.setPadding(15, 20, 15, 20);
        }

        final int no = j;

        ith_route.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ll_traffic_detail_route_container.removeAllViews();
                ll_detail_course_container.setVisibility(VISIBLE);

                mMap.clear();

                double alatitude = Double.parseDouble(arrival_lat);
                double alngtitude = Double.parseDouble(arrival_lng);

                LatLng Start = new LatLng(alatitude, alngtitude);

                start_m = mMap.addMarker(new MarkerOptions().position(Start).title("출발"));

                for (int i = 0; i < list_len[no]; i++) {

                    TextView tv_method_course = new TextView(PathFindActivity.this);
                    tv_method_course.setText(step[no][i].split(">")[0].trim());
                    tv_method_course.setTextSize(20);
                    tv_method_course.setTextColor(Color.BLACK);
                    tv_method_course.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) tv_method_course.getLayoutParams();
                    params1.gravity = Gravity.LEFT;
                    tv_method_course.setLayoutParams(params1);
                    tv_method_course.setBackgroundColor(Color.WHITE);
                    tv_method_course.setPadding(25, 25, 25, 25);
                    ll_traffic_detail_route_container.addView(tv_method_course);

                    TextView tv_detail_course = new TextView(PathFindActivity.this);
                    tv_detail_course.setText(getInstructions[no][i]);
                    tv_detail_course.setTextSize(18);
                    tv_detail_course.setTextColor(Color.BLACK);
                    tv_detail_course.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) tv_detail_course.getLayoutParams();
                    params2.gravity = Gravity.LEFT;
                    params2.setMargins(0, 0, 0, 30);
                    tv_detail_course.setLayoutParams(params2);
                    tv_detail_course.setPadding(5, 5, 20, 15);
                    ll_traffic_detail_route_container.addView(tv_detail_course);

                    ArrayList<LatLng> path_points = decodePolyPoints(getPolyline[no][i]); // 폴리라인 포인트 디코드 후 ArrayList에 저장

                    Polyline line = null;

                    if (line == null) {
                        line = mMap.addPolyline(new PolylineOptions()
                                .color(Color.rgb(58, 122, 255))
                                .geodesic(true)
                                .addAll(path_points));
                    } else {
                        line.remove();
                        line = mMap.addPolyline(new PolylineOptions()
                                .color(Color.rgb(58, 122, 255))
                                .geodesic(true)
                                .addAll(path_points));
                    }

                    if (goingE_lat[no][i] != null) {

                        double gelatitude = Double.parseDouble(goingE_lat[no][i]);
                        double gelngtitude = Double.parseDouble(goingE_lng[no][i]);
                        String Transit_n = TransitName[no][i];
                        String next_Transit_n = null;

                        if (i + 1 < list_len[no]) {
                            if (TransitName[no][i + 1] != null)
                                next_Transit_n = TransitName[no][i + 1];
                        }

                        LatLng GoingE = new LatLng(gelatitude, gelngtitude);

                        if (Transit_n == null) {
                            Transit_n = "도보";
                            if (next_Transit_n != null) {
                                marker_arr[1][i] = mMap.addMarker(new MarkerOptions().position(GoingE).title(Transit_n + " 후, " + next_Transit_n + " 승차"));
                            } else {
                                if (i == list_len[no] - 1) {
                                    marker_arr[1][i] = mMap.addMarker(new MarkerOptions().position(GoingE).title(Transit_n + " 후, 도착"));
                                }
                                marker_arr[1][i] = mMap.addMarker(new MarkerOptions().position(GoingE).title(Transit_n));
                            }
                        } else {
                            if (i == list_len[no] - 1) {
                                marker_arr[1][i] = mMap.addMarker(new MarkerOptions().position(GoingE).title(Transit_n + " 하차 후, 도착"));
                            }
                            marker_arr[1][i] = mMap.addMarker(new MarkerOptions().position(GoingE).title(Transit_n + " 하차"));
                        }
                        onMapReady(mMap);
                    }

                    if (goingS_lat[no][i] != null) {

                        double gslatitude = Double.parseDouble(goingS_lat[no][i]);
                        double gslngtitude = Double.parseDouble(goingS_lng[no][i]);
                        String Transit_n = TransitName[no][i];
                        String prev_Transit_n = null;
                        if (i != 0) {
                            prev_Transit_n = TransitName[no][i - 1];
                        }

                        LatLng GoingS = new LatLng(gslatitude, gslngtitude);

                        if (Transit_n == null) {
                            Transit_n = "도보";
                            // 도보 전 지하철 하차, 버스 하차 등을 표시할 수 있도록
                            if (prev_Transit_n != null) {
                                marker_arr[0][i] = mMap.addMarker(new MarkerOptions().position(GoingS).title(prev_Transit_n + "하차 후, " + Transit_n));
                            } else {
                                marker_arr[0][i] = mMap.addMarker(new MarkerOptions().position(GoingS).title(Transit_n));
                            }
                        } else {
                            marker_arr[0][i] = mMap.addMarker(new MarkerOptions().position(GoingS).title(Transit_n + " 승차"));
                        }
                        onMapReady(mMap);
                    }
                }

                double dlatitude = Double.parseDouble(destination_lat);
                double dlngtitude = Double.parseDouble(destination_lng);
                LatLng End = new LatLng(dlatitude, dlngtitude);
                end_m = mMap.addMarker(new MarkerOptions().position(End).title("도착"));
                onMapReady(mMap);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(End));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        });
    }
    /********************************위치 퍼미션 및 이동 ********************************/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng SOONGSIL = new LatLng(37.494509, 126.959740);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SOONGSIL));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        //위치사용 체크 후 현위치로 중심 이동
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        } else {
            checkLocationPermissionWithRationale();
        }
    }
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    //위치정보 사용여부 확인
    private void checkLocationPermissionWithRationale() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("위치정보")
                        .setMessage("이 앱을 사용하기 위해서는 위치정보에 접근이 필요합니다. 위치정보 접근을 허용하여 주세요.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(PathFindActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        }).create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    //현위치로 중심 이동
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public class Task extends AsyncTask<String, Void, String> {

        private String str, receiveMsg;

        @Override
        protected String doInBackground(String... params) {
            URL url = null;
            try {
                url = new URL(str_url);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (conn.getResponseCode() == conn.HTTP_OK) {
                    InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuffer buffer = new StringBuffer();
                    while ((str = reader.readLine()) != null) {
                        buffer.append(str);
                    }
                    receiveMsg = buffer.toString();

                    reader.close();
                } else {
                    Log.i("통신 결과", conn.getResponseCode() + "에러");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return receiveMsg;
        }
    }

}