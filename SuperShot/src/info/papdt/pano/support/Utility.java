package info.papdt.pano.support;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static info.papdt.pano.processor.ScreenshotComposer.DEBUG;

public class Utility
{
    private static final String TAG = Utility.class.getSimpleName();

    public static <T> int arrayContainsEx(T[] list, T[] sub, float threshold) {
        int length = sub.length;

        int index = -1;
        for (int i = 0; i < list.length - length; i++) {
            if (arrayCompareEx(list, sub, i, 0, length, threshold)) {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * 
     * @param a   队列1
     * @param b   队列2
     * @param aStart   起点1
     * @param bStart   地点2
     * @param length   比较长度
     * @param threshold   精度
     * @return true 表示相同
     */
    public static <T> boolean arrayCompareEx(T[] a, T[] b, int aStart, int bStart, int length, float threshold) {
        int unmatches = 0;
        for (int i = 0; i < length; i++) {
            T valueA = a[aStart + i];
            T valueB = b[bStart + i];

            if (valueA == null || !valueA.equals(valueB)) {
                unmatches++;
            }
        }

        /*if (DEBUG) {
            Log.d(TAG, "ummatches: " + unmatches + " of " + length);
        }*/

        return unmatches <= length * threshold;
    }


    /**
     * Find the longest common subarray
     * Designed for arrays that have common parts at head and tail
     * @param headArray The array that have a common part with the other at head
     * @param tailArray The array that have a common part with the other at tail
     *
     */
    public static <T> int arrayHeadTailMatch(T[] headArray, T[] tailArray, int length, float threshold) {
        if (headArray.length != tailArray.length) throw new IllegalArgumentException("length differs");

        long startTime = -1;

        if (DEBUG) {
            startTime = System.currentTimeMillis();
        }

        int arrayLength = headArray.length;
        /*int unmatches = 0;
        float thresholdValue = (float) length * threshold;
        */
        int ret = -1;

        //nLen = length，从整幅图片开始比较
        for (int nLen = length; nLen > 0; nLen--) {
            int j = arrayLength - nLen - 1;

            //从headArray的头（0）开始，tailArray的(尾-nlen)开始，比较nLen的长度
            if (arrayCompareEx(headArray, tailArray, 0, j, nLen, threshold)) {
                ret = nLen;
                break;
            }

            /*if (!headArray.get(i).equals(tailArray.get(j))) {
                unmatches++;

                if (unmatches > thresholdValue) {
                    ret = i - 1;
                    break;
                }
            }*/
        }

        if (DEBUG) {
            Log.d(TAG, "arrayHeadTailMatch time: " + (System.currentTimeMillis() - startTime));
        }

        return ret;
    }

    public static void notifyMediaScanner(Context context, String path) {
        Intent i = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        i.setData(Uri.fromFile(new File(path)));
        context.sendBroadcast(i);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }

    public static int dp2pxY(Context context, int dip) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dip * (displayMetrics.ydpi / 160.0f));
        return px;
    }
}
