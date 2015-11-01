package checker.app.com.skiranker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    SensorManager mSensorManager;
    TextView debugValue1;
    TextView debugValue2;
    TextView timeValue;
    TextView speedValue;
    TextView fallValue;
    TextView infoLabel;

    Button runButton;
    Button resetButton;
    boolean isChecking = true;
    double xAccel = 0.0;
    double yAccel = 0.0;
    double zAccel = 0.0;
    double xGravity = 0.0;
    double yGravity = 0.0;
    double zGravity = 0.0;
    double dx2dt = 0.0;
    double dy2dt = 0.0;
    double dz2dt = 0.0;
    double dxdt = 0.0;
    double dydt = 0.0;
    double dzdt = 0.0;

    double iniYAngle = 0.0;
    double iniZAngle = 0.0;
    double zAngle = 0.0;
    double yAngle = 0.0;

    double abs=0.0;
    double maxSpeed = 0.0;
    double vAveX = 0.0;
    double vAveY = 0.0;
    double vAveZ = 0.0;
    double aAveX = 0.0;
    double aAveY = 0.0;
    double aAveZ = 0.0;

    int countFall = 0;
    long startTimeStamp = 0;
    long nextTimeStamp = 0 ;
    long lastTimeStamp = 0;
    long totalTime = 0;
    long storedTime = 0;

    String sensorStr = "";
    boolean gravity = false;
    boolean orientation = false;
    boolean sensing = false;
    boolean justStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeValue = (TextView) findViewById(R.id.timeValue);
        speedValue = (TextView) findViewById(R.id.speedValue);
        fallValue = (TextView) findViewById(R.id.fallValue);
        infoLabel = (TextView) findViewById(R.id.infoLabel);

//        debugValue1 = (TextView) findViewById(R.id.debugValue1);
//        debugValue2 = (TextView) findViewById(R.id.debugValue2);

        runButton = (Button) findViewById(R.id.runButton);
        resetButton = (Button) findViewById(R.id.resetButton);

        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensing) {
                    sensing = false;
                    runButton.setText("START");
                    storedTime = totalTime;
                } else {
                    sensing = true;
                    runButton.setText("STOP");
                    startTimeStamp = System.currentTimeMillis();
                    justStarted = true;
                    lastTimeStamp = System.currentTimeMillis();
                }
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                resetStoredData();
                updateView();
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        nextTimeStamp = System.currentTimeMillis() + 10*1000;

    }

    public void onSensorChanged(SensorEvent event){
        if (sensing){
            if ( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                xAccel = event.values[0];
                yAccel = event.values[1];
                zAccel = event.values[2];
            }
            if (gravity && event.sensor.getType() == Sensor.TYPE_GRAVITY){
                xGravity = event.values[0];
                yGravity = event.values[1];
                zGravity = event.values[2];
                yAngle = Math.toDegrees(Math.asin(-1.0*yGravity/10.07));
                zAngle = Math.toDegrees(Math.asin(xGravity/10.07));
            }
            if (orientation && event.sensor.getType() == Sensor.TYPE_ORIENTATION){
                yAngle = event.values[1];
                zAngle = event.values[2];
                xGravity = 10.07 * Math.sin(Math.toRadians(zAngle));
                yGravity = - 10.07 * Math.sin(Math.toRadians(yAngle));
                zGravity = 10.07 * Math.cos(Math.toRadians(yAngle)) * Math.cos(Math.toRadians(zAngle));
            }
            if (justStarted){
                iniYAngle = yAngle;
                iniZAngle = zAngle;
                justStarted = false;
            }
            dx2dt = xAccel - xGravity;
            dy2dt = yAccel - yGravity;
            dz2dt = zAccel - zGravity;

            double dt = (System.currentTimeMillis() - lastTimeStamp)/1000.0;
            lastTimeStamp = System.currentTimeMillis();

            aAveX = (aAveX * totalTime + ( dx2dt * dt * 1000.0) ) / (totalTime + (dt * 1000.0) );
            aAveY = (aAveY * totalTime + ( dy2dt * dt * 1000.0) ) / (totalTime + (dt * 1000.0) );
            aAveZ = (aAveZ * totalTime + ( dz2dt * dt * 1000.0) ) / (totalTime + (dt * 1000.0) );

            dx2dt -= aAveX;
            dy2dt -= aAveY;
            dz2dt -= aAveZ;

            dxdt += dx2dt * dt;
            dydt += dy2dt * dt;
            dzdt += dx2dt * dt;

            abs = Math.sqrt(dx2dt*dx2dt + dy2dt*dy2dt + dz2dt*dz2dt);
            if (isChecking && (Math.abs(yAngle-iniYAngle)+Math.abs(zAngle-iniZAngle)) > 85.0 && System.currentTimeMillis() > nextTimeStamp){
                isChecking = false;
                nextTimeStamp = System.currentTimeMillis() + 10*1000;
                countFall++;
                fallValue.setText(countFall + " 回");
            }
            if (!isChecking && (Math.abs(yAngle-iniYAngle)+Math.abs(zAngle-iniZAngle)) < 30.0 && System.currentTimeMillis() > nextTimeStamp){
                isChecking = true;
                iniYAngle = yAngle;
                iniZAngle = zAngle;
                justStarted = true;
            }
            totalTime = sensing? System.currentTimeMillis() - startTimeStamp + storedTime: storedTime ;
            vAveX = (vAveX * totalTime + ( dxdt * dt * 1000.0) ) / (totalTime + (dt * 1000.0) );
            vAveY = (vAveY * totalTime + ( dydt * dt * 1000.0) ) / (totalTime + (dt * 1000.0) );
            vAveZ = (vAveZ * totalTime + ( dzdt * dt * 1000.0) ) / (totalTime + (dt * 1000.0) );
            dxdt -= vAveX;
            dydt -= vAveY;
            dzdt -= vAveZ;

            maxSpeed = getAbsVelocity(dxdt, dydt, dzdt)>maxSpeed? getAbsVelocity(dxdt, dydt, dzdt):maxSpeed;
        }

        updateView();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<List<Sensor>> allSensorList = new ArrayList<>();
        allSensorList.add(mSensorManager.getSensorList(Sensor.TYPE_ALL));
        for (List<Sensor> sensor: allSensorList){
            if (sensor.size()>0){
                for( Sensor aSensor: sensor){
                    if(aSensor.getType()==Sensor.TYPE_GRAVITY){
                        gravity = true;
                        orientation = false;
                    }else if(aSensor.getType()==Sensor.TYPE_ORIENTATION){
                        orientation = !gravity;
                    }
                }
            }
        }
        infoLabel.setText(gravity? "重力センサー使用":"傾きセンサー使用");

        sensorStr = "";
        for (List<Sensor> sensor: allSensorList){
            if (0 < sensor.size()){
                for (Sensor aSensor: sensor){
                    sensorStr += aSensor.getName() + "\n";
                    mSensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_UI);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    private void updateView(){
        timeValue.setText(totalTime/3600000 + "時間 " + totalTime%3600000/60000 + "分 " + totalTime%60000/1000 + "秒");
        speedValue.setText(String.format("%.2f", 3.6 * maxSpeed) + " km/h");
        fallValue.setText(countFall + " 回");

//        debugValue1.setText("dxdt:" + dxdt + "\r\ndydt:" + dydt + "\r\ndzdt:" + dzdt);
//        debugValue1.setText("角度差:" + Math.abs(yAngle - iniYAngle) + Math.abs(zAngle - iniZAngle));
//        debugValue2.setText("vAveX: " + vAveX + "\r\nvAveY : " + vAveY);
    }
    private void resetStoredData(){
        countFall = 0;
        maxSpeed = 0.0;
        storedTime = 0;
        totalTime = 0;
        dxdt = 0.0;
        dydt = 0.0;
        dzdt = 0.0;
    }
    private double getAbsVelocity(double vx, double vy, double vz){
        return Math.sqrt(dxdt*dxdt+dydt*dydt+dzdt*dzdt);
    }
}
