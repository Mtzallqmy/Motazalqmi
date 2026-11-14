#include <jni.h>
JNIEXPORT jstring JNICALL Java_com_moataz_motazalqmi_NativeIdentity_abi(JNIEnv *env, jclass unused) {
    (void)unused;
    return (*env)->NewStringUTF(env, "arm64-v8a");
}
