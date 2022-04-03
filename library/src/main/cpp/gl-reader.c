#include <jni.h>

#include <android/bitmap.h>
#include <GLES2/gl2.h>
#include <android/log.h>
#include <time.h>

// from android samples
/* return current time in milliseconds */
static double now_ms(void) {

    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;

}

JNIEXPORT void JNICALL
Java_eightbitlab_com_blurview_gl_GlReader_readToBitmap(JNIEnv *jenv, jclass thiz,
                                                                       jobject src) {
    unsigned char *srcByteBuffer;
    AndroidBitmapInfo srcInfo;

    int result = AndroidBitmap_getInfo(jenv, src, &srcInfo);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "readToBitmap", "get info failed : %d", result);
        return;
    }

    result = AndroidBitmap_lockPixels(jenv, src, (void **) &srcByteBuffer);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "readToBitmap", "lock pixels failed : %d", result);
        return;
    }

    double start = now_ms(); // start time

    glReadPixels(0, 0, srcInfo.width, srcInfo.height, GL_RGBA, GL_UNSIGNED_BYTE, srcByteBuffer);

    double end = now_ms(); // finish time

    double delta = end - start; // time your code took to exec in ms
    __android_log_print(ANDROID_LOG_ERROR, "LOL", "glReadPixels time : %f", delta);

    AndroidBitmap_unlockPixels(jenv, src);
}