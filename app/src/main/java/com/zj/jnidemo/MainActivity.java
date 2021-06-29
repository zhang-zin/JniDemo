package com.zj.jnidemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        changeName("李四");
        Person person = getPerson();
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(person.toString());

        get();
    }


    public native void get();

    public native void changeName(String name);

    /**
     * Java 将数据传递到 native 中
     */
    public native void test(
            boolean b,
            byte b1,
            char c,
            short s,
            long l,
            float f,
            double d,
            String name,
            int age,
            int[] i,
            String[] strs,
            Person person,
            boolean[] bArray
    );

    /**
     * C++生成Person对象
     *
     * @return person
     */
    public native Person getPerson();

}