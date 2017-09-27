package test.nexa.com.test;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;
//        GLCubeView gcv = new GLCubeView(context);
//        setContentView(gcv);


        setContentView(R.layout.activity_main);
    }
}
