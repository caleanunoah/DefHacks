package ai.fritz.poseestimationdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import ai.fritz.camera.MainActivity;

public class DataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
    }

    public void fun4(View v){
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("key","1");
        startActivity(i);

    }

}
