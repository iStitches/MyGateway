package org.xjx.common.config;

public class AbstractServiceInvoker implements ServiceInvoker{
    protected String invokerPath;
    protected long timeOut = 5000;

    @Override
    public String getInvokerPath() {
        return invokerPath;
    }

    @Override
    public void setInvokerPath(String path) {
        this.invokerPath = path;
    }

    @Override
    public long getTimeOut() {
        return timeOut;
    }

    @Override
    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }
}
