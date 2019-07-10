package com.sd.lib.dldmgr;

public class TransmitParam
{
    private static final long CALCULATE_SPEED_INTERVAL = 100;

    private long mCurrent;
    private long mTotal;
    private int mProgress;
    private int mSpeedBps;

    private long mLastTime;
    private long mLastCount;

    private long mCalculateSpeedInterval = CALCULATE_SPEED_INTERVAL;

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
        }

        mProgress = (int) (current * 100 / total);
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
     * 设置计算速率的间隔
     *
     * @param calculateSpeedInterval
     */
    public void setCalculateSpeedInterval(long calculateSpeedInterval)
    {
        if (calculateSpeedInterval <= 0)
            calculateSpeedInterval = CALCULATE_SPEED_INTERVAL;

        mCalculateSpeedInterval = calculateSpeedInterval;
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

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(getCurrent()).append("/").append(getTotal()).append("\r\n")
                .append(getProgress()).append("%").append("\r\n")
                .append(getSpeedKBps()).append("KBps");
        return sb.toString();
    }
}
