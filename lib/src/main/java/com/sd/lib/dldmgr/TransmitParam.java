package com.sd.lib.dldmgr;

public class TransmitParam
{
    private final long mCalculateSpeedInterval;

    private long mCurrent;
    private long mTotal;
    private int mProgress;
    private int mSpeedBps;

    private long mLastTime;
    private long mLastCount;

    public TransmitParam()
    {
        this(0);
    }

    public TransmitParam(long calculateSpeedInterval)
    {
        if (calculateSpeedInterval <= 0)
            calculateSpeedInterval = 100;
        mCalculateSpeedInterval = calculateSpeedInterval;
    }

    public TransmitParam copy()
    {
        final TransmitParam copy = new TransmitParam(this.mCalculateSpeedInterval);
        copy.mCurrent = this.mCurrent;
        copy.mTotal = this.mTotal;
        copy.mProgress = this.mProgress;
        copy.mSpeedBps = this.mSpeedBps;

        copy.mLastTime = this.mLastTime;
        copy.mLastCount = this.mLastCount;
        return copy;
    }

    /**
     * 传输
     *
     * @param total
     * @param current
     * @return true-进度增加了
     */
    synchronized boolean transmit(long total, long current)
    {
        final int oldProgress = mProgress;

        mTotal = total;
        mCurrent = current;

        if (mTotal <= 0)
        {
            mProgress = 0;
        } else
        {
            final long currentTime = System.currentTimeMillis();
            final long timeInterval = currentTime - mLastTime;
            if (timeInterval >= mCalculateSpeedInterval)
            {
                final long count = current - mLastCount;
                mSpeedBps = (int) (count * (1000f / timeInterval));

                mLastTime = currentTime;
                mLastCount = current;
            }
            mProgress = (int) (current * 100 / total);
        }

        return mProgress > oldProgress;
    }

    /**
     * 传输是否完成
     *
     * @return
     */
    public boolean isComplete()
    {
        return mCurrent == mTotal && mCurrent > 0;
    }

    /**
     * 当前传输量
     *
     * @return
     */
    public long getCurrent()
    {
        return mCurrent;
    }

    /**
     * 总量
     *
     * @return
     */
    public long getTotal()
    {
        return mTotal;
    }

    /**
     * 传输进度
     *
     * @return [0-100]
     */
    public int getProgress()
    {
        return mProgress;
    }

    /**
     * 传输速率(Bps)
     *
     * @return
     */
    public int getSpeedBps()
    {
        return mSpeedBps;
    }

    /**
     * 传输速率(KBps)
     *
     * @return
     */
    public int getSpeedKBps()
    {
        return getSpeedBps() / 1024;
    }
}
