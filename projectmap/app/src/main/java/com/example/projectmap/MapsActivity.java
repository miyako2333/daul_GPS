package com.example.projectmap;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.projectmap.databinding.ActivityMapsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    SupportMapFragment mFragment;
    GoogleMap mMap;
    ActivityMapsBinding binding;
    Marker mCurrLocation;

    // 主线程Handler
    // 用于将从服务器获取的消息显示出来
    private Handler mMainHandler;
    // Socket变量
    private Socket socket;

    // 线程池
    // 为了方便展示,此处直接采用线程池进行线程管理,而没有一个个开线程
    private ExecutorService mThreadPool;
    /**
     * 接收服务器消息 变量
     */
    // 输入流对象
    InputStream is;

    // 输入流读取器对象
    InputStreamReader isr ;
    BufferedReader br ;

    // 接收服务器发送过来的消息
    String response;

    // 输出流对象
    OutputStream outputStream;

    // 连接 断开连接 发送数据到服务器 的按钮变量
    private Button btnConnect, btnDisconnect, btnSend;
    private Button btnRecord, btnDelete, btnOutput;

    // 显示接收服务器消息 按钮
    private TextView Receive,receive_message, respond;
    private TextView name2, lat2, lng2, alt2;

    // 输入需要发送的消息 输入框
    private EditText mEdit;
    private EditText filename;
    private JSONObject IOUtils;

    // Lnglat
    String name, lat, lng;
    Integer nameNumber = 0;
    String nameFile;
    String nameOrder;
    String nameNow = "Field1";

    String jsonString = "";
    Dictionary recording = new Hashtable();
    Dictionary Read_recording = new Hashtable();

    Integer adder = 1;
    private int EXTERNAL_STORAGE_PERMISSION_CODE = 23;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        // Retrieve the content view that renders the map.
//        setContentView(R.layout.activity_maps);
//
//        // Get the SupportMapFragment and request notification when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        /**
         * 初始化操作
         */

        // 初始化所有按钮
        btnConnect = (Button) findViewById(R.id.connect);
        btnDisconnect = (Button) findViewById(R.id.disconnect);
        Receive = (Button) findViewById(R.id.Receive);
        respond = (TextView) findViewById(R.id.respond);
        mEdit = (EditText) findViewById(R.id.edit);
        btnRecord = (Button) findViewById(R.id.record);
        btnDelete = (Button) findViewById(R.id.delete);
        btnOutput = (Button) findViewById(R.id.output);
        name2 = (TextView) findViewById(R.id.name2);
        lat2 = (TextView) findViewById(R.id.lat2);
        lng2 = (TextView) findViewById(R.id.lng2);
        alt2 = (TextView) findViewById(R.id.height2);
        filename = (EditText) findViewById(R.id.filename);

        // 初始化线程池
        mThreadPool = Executors.newCachedThreadPool();


        /**
         * 创建客户端 & 服务器的连接
         */
//        btnConnect.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // 利用线程池直接开启一个线程 & 执行该线程
//                mThreadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        String currdata;
//                        currdata = createTXT();
//                        autoSave(currdata);
//                    }
//                });
//            }
//        });

        final int Time = 30*1000;    //时间间隔，   单位 ms
        int N = 0;      //用来观测重复执行

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, Time);
                //每隔一段时间要重复执行的代码
                String currdata;
                currdata = createTXT();
                autoSave(currdata);
            }
        };
        handler.postDelayed(runnable, Time);	//启动计时器


        /**
         * 创建客户端 & 服务器的连接
         */
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 利用线程池直接开启一个线程 & 执行该线程
                mThreadPool.execute(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {

                        try {
                            String hhost = mEdit.getText().toString();
                            // 创建Socket对象 & 指定服务端的IP 及 端口号
//                            if(hhost == "196.168.5.1"){
//                                hhost = "100.64.13.175";
//                            }
//                            String hhost = "100.64.13.175";
                            socket = new Socket(hhost, 65432);
                            System.out.println("------------------------------------");
                            System.out.println(hhost);
                            System.out.println("------------------------------------");
                            // 判断客户端和服务器是否连接成功
                            respond.setText(String.format("Try : %s", hhost));
                            System.out.println(socket.isConnected());

                        } catch (IOException e) {
                            e.printStackTrace();
                            respond.setText("Disconnected");
                            System.exit(-1);
                        }

                        while (true){
                            try {
                                if (!socket.isConnected()){
                                    respond.setText("Disconnected");
                                    socket.close();;
                                }
                            } catch (IOException e) {
                                respond.setText("Disconnected");
                                e.printStackTrace();
                            }
                        }
                    }
                });

            }
        });

        /**
         * 接收 服务器消息
         */
        Receive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // 利用线程池直接开启一个线程 & 执行该线程
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
//                        LatLng sydney1 = new LatLng(-38, 151);
//                        Marker marker = mMap.addMarker(new MarkerOptions().position(sydney1));
//                        onLocationChanged(-38,151);
                        while (true) {

                            try {

                                // 步骤1：创建输入流对象InputStream
                                is = socket.getInputStream();

                                // 步骤2：创建输入流读取器对象 并传入输入流对象
                                // 该对象作用：获取服务器返回的数据
                                isr = new InputStreamReader(is);
                                br = new BufferedReader(isr);
                                StringBuilder sb = new StringBuilder();

//                            String line;
////                            br = new BufferedReader(new InputStreamReader(is));
//                            while ((line = br.readLine()) != null) {
//                                sb.append(line);
//                            }
                                String line = br.readLine();
//                            System.out.println("------------------------------------");
//                            System.out.println(line);
//                            System.out.println("------------------------------------");
//                            line = "{\"name\": \"Tokyo\", \"lat\": \"35.652832\", \"lng\": \"139.839478\"}";
                                JSONObject reader = new JSONObject(line);

                                name = reader.getString("name");
                                lat = reader.getString("lat");
                                lng = reader.getString("lng");

                                if (adder == 1) {
                                    respond.setText("Connected");
                                }

                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

        /**
         * 断开客户端 & 服务器的连接
         */
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Double currlat = Double.parseDouble(lat);
                Double currlng = Double.parseDouble(lng);
                onLocationChanged(currlat,currlng);
//                onLocationChanged(currlat,currlng);
                LatLng curr = new LatLng(currlat, currlng);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(curr));

//                onLocationChanged(-38,151);
//                try {
//                    // 断开 客户端发送到服务器 的连接，即关闭输出流对象OutputStream
//                    outputStream.close();
//
//                    // 断开 服务器发送到客户端 的连接，即关闭输入流读取器对象BufferedReader
//                    br.close();
//
//                    // 最终关闭整个Socket连接
//                    socket.close();
//
//                    // 判断客户端和服务器是否已经断开连接
//                    System.out.println(socket.isConnected());
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String tempfilename = filename.getText().toString();


                if (nameNow.equals(tempfilename) ) {
                    nameNumber = nameNumber + 1;
                }
                else{
                    nameNumber = 1;
                    nameNow = tempfilename;
                }

                nameOrder = tempfilename + "_" + nameNumber.toString();

                ArrayList  tempName, testName;
                Object tempdic, testdic;

                if(adder > 1){
                    testdic = recording.get(adder-1);
                    testName = (ArrayList)testdic;
                    String oldLat = (String) testName.get(1);
                    if (oldLat == lat){
                        respond.setText("POS NOT CHANGE");
                    }else{
                        respond.setText("Connected");
                    }
                }

                ArrayList<String> Read_dic = new ArrayList<String>();
                String Read_nameOrder = nameOrder;
                String Read_lat, Read_lng, Read_name;
                if (lat.length()>9){
                    Read_lat = lat.substring(0,9);
                }else{
                    Read_lat = lat;
                }
                if (lng.length()>9){
                    Read_lng = lng.substring(0,9);
                }else{
                    Read_lng = lng;
                }
                if (name.length()>6){
                    Read_name = name.substring(0,5);
                }else{
                    Read_name = name;
                }

                Read_dic.add(Read_nameOrder);
                Read_dic.add(Read_lat);
                Read_dic.add(Read_lng);
                Read_dic.add(Read_name);

                Read_recording.put(adder, Read_dic);

                ArrayList<String> dic = new ArrayList<String>();
                dic.add(nameOrder);
                dic.add(lat);
                dic.add(lng);
                dic.add(name);

                recording.put(adder, dic);

                String displayName = "", displayLat = "", displayLng = "", displayAlt = "";

                Integer i = adder;
                if (adder <= 10){
                    while (i > 0){
                        tempdic = Read_recording.get(i);
                        tempName = (ArrayList)tempdic;
                        displayName = displayName + tempName.get(0) + "\n";
                        displayLat = displayLat + tempName.get(1) + "\n";
                        displayLng = displayLng + tempName.get(2) + "\n";
                        displayAlt = displayAlt + tempName.get(3) + "\n";
                        i -= 1;
                    }
                }
                else{
                    while (i > adder - 10){
                        tempdic = Read_recording.get(i);
                        tempName = (ArrayList)tempdic;
                        displayName = displayName + tempName.get(0) + "\n";
                        displayLat = displayLat + tempName.get(1) + "\n";
                        displayLng = displayLng + tempName.get(2) + "\n";
                        displayAlt = displayAlt + tempName.get(3) + "\n";
                        i -= 1;
                    }
                }
//                System.out.println("------------------------------------");
//                System.out.println(displayName);
//                System.out.println("------------------------------------");
                name2.setText(displayName);
                lat2.setText(displayLat);
                lng2.setText(displayLng);
                alt2.setText(displayAlt);
                adder += 1;
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameNumber = nameNumber - 1;

                adder -= 1;
                Read_recording.remove(adder);
                recording.remove(adder);
                String displayName = "", displayLat = "", displayLng = "", displayAlt = "";
                ArrayList  tempName;
                Object tempdic;
                Integer i = adder-1;
                if (adder <= 10){
                    while (i > 0){
                        tempdic = Read_recording.get(i);
                        tempName = (ArrayList)tempdic;
                        displayName = displayName + tempName.get(0) + "\n";
                        displayLat = displayLat + tempName.get(1) + "\n";
                        displayLng = displayLng + tempName.get(2) + "\n";
                        displayAlt = displayAlt + tempName.get(3) + "\n";
                        i -= 1;
                    }
                }
                else{
                    while (i >= adder - 10){
                        tempdic = Read_recording.get(i);
                        tempName = (ArrayList)tempdic;
                        displayName = displayName + tempName.get(0) + "\n";
                        displayLat = displayLat + tempName.get(1) + "\n";
                        displayLng = displayLng + tempName.get(2) + "\n";
                        displayAlt = displayAlt + tempName.get(3) + "\n";
                        i -= 1;
                    }
                }
                name2.setText(displayName);
                lat2.setText(displayLat);
                lng2.setText(displayLng);
                alt2.setText(displayAlt);
            }
        });

        btnOutput.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                Integer i = adder-1;
                ArrayList  tempName;
                Object tempdic;
                String txt = "";
                while (i > 0 ){
                    tempdic = recording.get(i);
                    tempName = (ArrayList)tempdic;
                    txt = txt + tempName.get(0) + ", ";
                    txt = txt + tempName.get(1) + ", ";
                    txt = txt + tempName.get(2) + ", ";
                    txt = txt + tempName.get(3) + "\n";
                    i -= 1;
                }
                savePublicly(txt);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        System.out.println("------------------------------------");
//        System.out.println(name);
//        System.out.println(lat);
//        System.out.println("------------------------------------");
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
//        LatLng sydney = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(sydney);
        markerOptions.title("Marker in Sydney");
        mCurrLocation = mMap.addMarker(markerOptions);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12.0f));
//        mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
    }

    public void onLocationChanged(Double lat, Double lng) {
        mCurrLocation.remove();
        LatLng pos = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.title("New Position");
        mCurrLocation = mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 12.0f));

        //If you only need one location, unregister the listener
        //LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void savePublicly(String data) {
        // Requesting Permission to access External Storage
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                EXTERNAL_STORAGE_PERMISSION_CODE);

        // getExternalStoragePublicDirectory() represents root of external storage, we are using DOWNLOADS
        // We can use following directories: MUSIC, PODCASTS, ALARMS, RINGTONES, NOTIFICATIONS, PICTURES, MOVIES
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // Storing the data in file with name as geeksData.txt
        String tempfilename = filename.getText().toString();
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yy-mm-dd");
        String strDate = dateFormat.format(date);
        nameFile = tempfilename + "_" + strDate + ".txt";

        File file = new File(folder, nameFile);
        writeTextData(file, data);
    }

    public void autoSave(String data) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                EXTERNAL_STORAGE_PERMISSION_CODE);

        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File file = new File(folder, "Auto.txt");
        writeTextData(file, data);
    }

    public String createTXT() {
        Integer i = adder-1;
        ArrayList  tempName;
        Object tempdic;
        String txt = "";
        while (i > 0 ){
            tempdic = recording.get(i);
            tempName = (ArrayList)tempdic;
            txt = txt + tempName.get(0) + ", ";
            txt = txt + tempName.get(1) + ", ";
            txt = txt + tempName.get(2) + ", ";
            txt = txt + tempName.get(3) + "\n";
            i -= 1;
        }
        return txt;
    }

    // writeTextData() method save the data into the file in byte format
    // It also toast a message "Done/filepath_where_the_file_is_saved"
    private void writeTextData(File file, String data) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data.getBytes());
            Toast.makeText(this, "Done" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}