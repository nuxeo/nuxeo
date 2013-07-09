package org.nuxeo.runtime.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.impl.AbstractRuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class SimpleRuntimeContext extends AbstractRuntimeContext {

    public SimpleRuntimeContext() {
       super("simple");
    }

	public static  class SimpleBundle implements Bundle {
		@Override
		public int compareTo(Bundle o) {
			return 0;
		}

		@Override
		public int getState() {
			return 0;
		}

		@Override
		public void start(int options) throws BundleException {

		}

		@Override
		public void start() throws BundleException {

		}

		@Override
		public void stop(int options) throws BundleException {

		}

		@Override
		public void stop() throws BundleException {

		}

		@Override
		public void update(InputStream input) throws BundleException {

		}

		@Override
		public void update() throws BundleException {

		}

		@Override
		public void uninstall() throws BundleException {

		}

		@Override
		public Dictionary<String, String> getHeaders() {
			return null;
		}

		@Override
		public long getBundleId() {
			return 0;
		}

		@Override
		public String getLocation() {
			return null;
		}

		@Override
		public ServiceReference<?>[] getRegisteredServices() {
			return new ServiceReference[0];
		}

		@Override
		public ServiceReference<?>[] getServicesInUse() {
			return new ServiceReference[0];

		}

		@Override
		public boolean hasPermission(Object permission) {
			return true;
		}

		@Override
		public URL getResource(String name) {
			return null;
		}

		@Override
		public Dictionary<String, String> getHeaders(String locale) {
			return new Hashtable<String,String>();
		}

		@Override
		public String getSymbolicName() {
			return "";
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
 			return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return Thread.currentThread().getContextClassLoader().getResources(name);
		}

		@Override
		public Enumeration<String> getEntryPaths(String path) {
			return new EmptyEnumeration<String>();
		}

		@Override
		public URL getEntry(String path) {
			return null;
		}

		@Override
		public long getLastModified() {
			return 0;
		}

		@Override
		public Enumeration<URL> findEntries(String path, String filePattern,
				boolean recurse) {
			return new EmptyEnumeration<URL>();
		}

		@Override
		public BundleContext getBundleContext() {
			return new BundleContext() {

				@Override
				public String getProperty(String key) {
					return null;
				}

				@Override
				public Bundle getBundle() {
					return SimpleBundle.this;
				}

				@Override
				public Bundle installBundle(String location, InputStream input)
						throws BundleException {
					throw new UnsupportedOperationException();
				}

				@Override
				public Bundle installBundle(String location)
						throws BundleException {
					throw new UnsupportedOperationException();
				}

				@Override
				public Bundle getBundle(long id) {
					throw new UnsupportedOperationException();
				}

				@Override
				public Bundle[] getBundles() {
					return new Bundle[] { SimpleBundle.this };
				}

				@Override
				public void addServiceListener(ServiceListener listener,
						String filter) throws InvalidSyntaxException {

				}

				@Override
				public void addServiceListener(ServiceListener listener) {

				}

				@Override
				public void removeServiceListener(ServiceListener listener) {

				}

				@Override
				public void addBundleListener(BundleListener listener) {

				}

				@Override
				public void removeBundleListener(BundleListener listener) {

				}

				@Override
				public void addFrameworkListener(FrameworkListener listener) {

				}

				@Override
				public void removeFrameworkListener(FrameworkListener listener) {

				}

				@Override
				public ServiceRegistration<?> registerService(String[] clazzes,
						Object service, Dictionary<String, ?> properties) {
					throw new UnsupportedOperationException();
				}

				@Override
				public ServiceRegistration<?> registerService(String clazz,
						Object service, Dictionary<String, ?> properties) {
					throw new UnsupportedOperationException();
				}

				@Override
				public <S> ServiceRegistration<S> registerService(
						Class<S> clazz, S service,
						Dictionary<String, ?> properties) {
					throw new UnsupportedOperationException();
				}

				@Override
				public ServiceReference<?>[] getServiceReferences(String clazz,
						String filter) throws InvalidSyntaxException {
					return new ServiceReference[0];
				}

				@Override
				public ServiceReference<?>[] getAllServiceReferences(
						String clazz, String filter)
						throws InvalidSyntaxException {
					return new ServiceReference[0];
				}

				@Override
				public ServiceReference<?> getServiceReference(String clazz) {
					return null;
				}

				@Override
				public <S> ServiceReference<S> getServiceReference(
						Class<S> clazz) {
					return null;
				}

				@Override
				public <S> Collection<ServiceReference<S>> getServiceReferences(
						Class<S> clazz, String filter)
						throws InvalidSyntaxException {
					return Collections.emptySet();
				}

				@Override
				public <S> S getService(ServiceReference<S> reference) {
					return null;
				}

				@Override
				public boolean ungetService(ServiceReference<?> reference) {
					return false;
				}

				@Override
				public File getDataFile(String filename) {
					return null;
				}

				@Override
				public Filter createFilter(String filter)
						throws InvalidSyntaxException {
					throw new UnsupportedOperationException();
				}

				@Override
				public Bundle getBundle(String location) {
					return null;
				}

			};
		}

		@Override
		public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
				int signersType) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Version getVersion() {
			return Version.emptyVersion;
		}

		@Override
		public <A> A adapt(Class<A> type) {
			return null;
		}

		@Override
		public File getDataFile(String filename) {
			return null;
		}
	}

	public static class EmptyEnumeration<T> implements Enumeration<T> {
		@Override
		public boolean hasMoreElements() {
			return false;
		}

		@Override
		public T nextElement() {
			throw new IllegalStateException();
		}
	}

	Bundle bundle = new SimpleBundle();

	@Override
	public Bundle getBundle() {
		return bundle;
	}


}
