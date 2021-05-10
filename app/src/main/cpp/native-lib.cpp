#include <jni.h>
#include <string>
#include <android/log.h>
#include <iostream>

#define TAG "JNI"

//__VA_ARGS__ 代表可变参数
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" int main();

// 1.基本类型
// java类型       JNI类型         c/c++类型
// boolean       jboolean      unsigned char(无符号8位整型)
// byte          jbyte         char(有符号8位整型)
// char          jchar         unsingned short (无符号 16 位整型)
// short         jshort        short(有符号 16 位整型)
// int           jint          int(有符号 32 位整型)
// logn          jlong         long(有符号 64 位整型)
// float         jfloat        float(有符号 32 位浮点型)
// double        jdouble       double (有符号 64 位双精度型)

// 2.引用类型并不直接向原生代码公开

// 3.数据类型描述符
// 在JVM虚拟机中，存储数据类型的名称时，使用指定的描述符来存储。
// Java类型       签名(描述符)
// boolean        Z
// byte           B
// char           C
// short          S
// int            I
// long           J
// float          F
// double         D
// void           V
// 其它引用类型     L+全类名+;
// type[]         [
// method type    (参数)返回值

// 示例
/*
 * 表示一个String
 * java类型：java.lang.String
 * JNI描述符：Ljava/lang/String;
 *
 * 表示一个数组: String []             int [][]
 * JNI描述符：  [Ljava/lang/String;    [[I
 *
 * 表示一个方法:
 * Java方法： long fun(int n,String s,int[] aar);   void fun()
 * JNI描述符： (ILjava/lang/String;[I)J             ()V
 *
 * 也可以使用命令 : javap -s 全路径 来获取方法签名
 * */

extern "C" JNIEXPORT jstring JNICALL
Java_com_zj_jnidemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    jstring newStringUtf = env->NewStringUTF(hello.c_str());
    return newStringUtf;
}

extern "C" JNIEXPORT void JNICALL
Java_com_zj_jnidemo_MainActivity_changeName(JNIEnv *env, jobject thiz, jstring name) {
    jclass pJclass = env->GetObjectClass(thiz);
    jfieldID pId = env->GetFieldID(pJclass, "name", "Ljava/lang/String;");
    auto j_str = static_cast<jstring>(env->GetObjectField(thiz, pId));
    char *c_str = const_cast<char *>(env->GetStringUTFChars(j_str, NULL));

    LOGD("native : %s\n", c_str);
    env->SetObjectField(thiz, pId, name);
}

extern "C" JNIEXPORT void JNICALL
Java_com_zj_jnidemo_MainActivity_test(JNIEnv *env, jobject thiz, jboolean b, jbyte b1, jchar c,
                                      jshort s, jlong l, jfloat f, jdouble d, jstring name,
                                      jint age, jintArray i, jobjectArray strs, jobject person,
                                      jbooleanArray b_array) {
    //1. 接收java传递的boolean对象
    unsigned char b_boolean = b;
    LOGE("boolean-> %d", b_boolean);

    //2. 接收Java传递的byte值
    char c_byte = b1;
    LOGE("byte-> %d", c_byte);

    //3. 接收char
    unsigned short c_char = c;
    LOGE("char-> %d", c_char);

    //4. 接收short
    short c_short = s;
    LOGE("short-> %d", c_short);

    long c_long = l;
    LOGE("long-> %ld", c_long);

    float c_float = f;
    LOGE("long-> %f", c_float);

    double c_double = d;
    LOGE("double-> %lf", c_double);

    const char *name_string = env->GetStringUTFChars(name, 0);
    LOGE("string-> %s", name_string);
    env->ReleaseStringUTFChars(name, name_string);

    int age_java = age;
    LOGE("int-> %d", age);

    //10. 打印java传递的数组 int[]
    jint *intArray = env->GetIntArrayElements(i, NULL);
    //拿到数组长度
    jsize intArraySize = env->GetArrayLength(i);
    for (int i = 0; i < intArraySize; ++i) {
        LOGE("intArray-> %d", intArray[i]);
    }
    //释放数组
    env->ReleaseIntArrayElements(i, intArray, 0);

    //11. 打印String[]
    jsize stringArraySize = env->GetArrayLength(strs);
    for (int i = 0; i < stringArraySize; ++i) {
        jobject pJobject = env->GetObjectArrayElement(strs, i);
        //强转 jin string
        jstring data = static_cast<jstring>(pJobject);

        //转 c string
        const char *string = env->GetStringUTFChars(data, 0);
        LOGE("String[%d]: %s", i, string);
        //回收 String[]
        env->ReleaseStringUTFChars(data, string);
    }

    //12. 打印传递的object对象
    jclass pJclass = env->GetObjectClass(person);
    jmethodID pId = env->GetMethodID(pJclass, "getName", "()Ljava/lang/String;");

    jobject method = env->CallObjectMethod(person, pId);
    jstring pJstring = static_cast<jstring>(method);
    const char *chars = env->GetStringUTFChars(pJstring, 0);
    LOGE("person name: %s", chars);
    env->DeleteLocalRef(pJclass); // 回收
    env->DeleteLocalRef(person); // 回收

    //13.
    jsize b_arraySize = env->GetArrayLength(b_array);
    jboolean *elements = env->GetBooleanArrayElements(b_array, NULL);
    for (int i = 0; i < b_arraySize; ++i) {
        bool b = elements[i];
        jboolean b2 = elements[i];
        LOGE("boolean:%d", b);
        LOGE("jboolean:%d", b2);
    }
    //回收
    env->ReleaseBooleanArrayElements(b_array, elements, 0);

}

extern "C" JNIEXPORT jobject JNICALL
Java_com_zj_jnidemo_MainActivity_getPerson(JNIEnv *env, jobject thiz) {

    jclass pJclass = env->FindClass("com/zj/jnidemo/Person");
    const char *constructMethod = "<init>"; //Java构造函数标识

    //得到空参构造函数
    jmethodID pId = env->GetMethodID(pJclass, constructMethod, "()V");

    //创建Person实例
    jobject person = env->NewObject(pJclass, pId);

    //
    jmethodID setAgeMethodId = env->GetMethodID(pJclass, "setAge", "(I)V");
    jmethodID setNameMethodId = env->GetMethodID(pJclass, "setName", "(Ljava/lang/String;)V");

    jstring pJstring = env->NewStringUTF("王五");

    env->CallVoidMethod(person, setAgeMethodId, 29);
    env->CallVoidMethod(person, setNameMethodId, pJstring);

    const char *sig = "()Ljava/lang/String;";
    jmethodID jtoString = env->GetMethodID(pJclass, "toString", sig);
    jobject obj_string = env->CallObjectMethod(person, jtoString);
    jstring perStr = static_cast<jstring >(obj_string);
    const char *itemStr2 = env->GetStringUTFChars(perStr, NULL);
    LOGE("Person: %s", itemStr2);
    return person;

}extern "C"

JNIEXPORT void JNICALL
Java_com_zj_jnidemo_MainActivity_get(JNIEnv *env, jobject thiz) {
    int i = main();
    LOGE("native get: %d\n", i);
}