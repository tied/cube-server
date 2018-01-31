package com.mesilat.cube;

import com.atlassian.jira.ofbiz.DefaultOfBizConnectionFactory;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class CubeDataSource implements DataSource {
    private final DefaultOfBizConnectionFactory cf = new DefaultOfBizConnectionFactory();
    private PrintWriter logWriter = new PrintWriter(System.out);
    private int loginTimeout = 60;
    
    @Override
    public Connection getConnection() throws SQLException {
        return cf.getConnection();
    }
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return cf.getConnection();
    }
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }
    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}