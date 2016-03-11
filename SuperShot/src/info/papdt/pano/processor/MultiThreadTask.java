package info.papdt.pano.processor;

import android.util.Log;
import static info.papdt.pano.processor.ScreenshotComposer.DEBUG;

public abstract class MultiThreadTask<A, R>
{
    private static final String TAG = MultiThreadTask.class.getSimpleName();
    private A mArgument;
    private R[] mResult;
    private int mFinishCount = 0;
    private boolean mFinished = false;

    // Should pass in an empty array that have the length of the result
    public MultiThreadTask(A argument, R[] result) {//argument对应图片，result对应行数组
        mResult = result;
        mArgument = argument;
    }

    protected void setResult(int position, R value) {
        synchronized (mResult) {
            mResult[position] = value;
        }
    }

    public R[] execute(int threadCount) {
        if (mFinished) return null;

        int length = mResult.length;

        if (length <= threadCount) {
            threadCount = length;
        }

        int taskSize = length / threadCount;

        if (DEBUG) {
            Log.d(TAG, "length = " + length + " taskSize = " + taskSize);
        }

        //按照线程数，平均分配任务给n个线程
        for (int i = 0; i < threadCount; i++) {
            final int start = i * taskSize;
            int taskLength = taskSize;

            if (i == threadCount - 1) {
                taskLength = length - start;
            }

            final int task = taskLength;

            if (DEBUG) {
                Log.d(TAG, i + " start = " + start + " length = " + task + " end = " + (start + task));
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    doExecute(mArgument, start, task);
                    mFinishCount++;
                }
            }).start();
        }

        while (mFinishCount < threadCount); // Wait for finishing

        return mResult;
    }

    protected abstract void doExecute(A argument, int taskStart, int taskLength);

}
