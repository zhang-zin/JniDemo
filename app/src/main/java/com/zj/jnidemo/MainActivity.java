package com.zj.jnidemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private String name = "张三";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        changeName("李四");
        Person person = getPerson();
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(person.toString());

        test(false,
                (byte) 1,
                ',',
                (short) 3,
                4,
                3.3f,
                4.2d,
                "李四",
                28,
                new int[]{1, 2, 3},
                new String[]{"1", "2", "#"},
                new Person("张三"),
                new boolean[]{false, true, true});

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

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
     * @return person
     */
    public native Person getPerson();

}