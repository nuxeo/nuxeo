package org.nuxeo.runtime.datasource.geronimo;

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.security.auth.Subject;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.tranql.connector.AllExceptionsAreFatalSorter;
import org.tranql.connector.CredentialExtractor;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;
import org.tranql.connector.jdbc.ManagedXAConnection;

public class XADataSourceMCF extends AbstractXADataSourceMCF {

    protected final String username;

    protected final String password;

    protected XADataSourceMCF(XADataSource xaDataSource, String username, String password) {
        super(xaDataSource, new AllExceptionsAreFatalSorter());
        this.username = username;
        this.password = password;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        CredentialExtractor credentialExtractor = new CredentialExtractor(subject, connectionRequestInfo, this);

        XAConnection sqlConnection = getPhysicalConnection(subject, credentialExtractor);
        try {
            return new ManagedXAConnection(this, sqlConnection, credentialExtractor, exceptionSorter);
        } catch (SQLException e) {
            throw new ResourceAdapterInternalException("Could not set up ManagedXAConnection", e);
        }
    }

}
