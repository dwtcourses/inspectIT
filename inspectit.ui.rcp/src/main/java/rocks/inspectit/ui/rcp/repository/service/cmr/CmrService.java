package rocks.inspectit.ui.rcp.repository.service.cmr;

import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import rocks.inspectit.shared.all.serializer.provider.SerializationManagerProvider;
import rocks.inspectit.ui.rcp.repository.CmrRepositoryDefinition;

/**
 * Abstract class for all {@link CmrRepositoryDefinition} service classes.
 *
 * @author Ivan Senic
 *
 */
public class CmrService implements ICmrService {

	/**
	 * Protocol used.
	 */
	private static final String PROTOCOL = "http://";

	/**
	 * Remoting path.
	 */
	private static final String REMOTING = "/remoting/";

	/**
	 * {@link CmrRepositoryDefinition}.
	 */
	private CmrRepositoryDefinition cmrRepositoryDefinition;

	/**
	 * Real service where calls will be executed.
	 */
	private Object service;

	/**
	 * Service interface.
	 */
	private Class<?> serviceInterface;

	/**
	 * Service name.
	 */
	private String serviceName;

	/**
	 * The serialization manager for kryo.
	 */
	private SerializationManagerProvider serializationManagerProvider;

	/**
	 * Defines if the default value should be returned when communication errors occurs in the
	 * invocation of the service.
	 */
	private boolean defaultValueOnError;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initService(CmrRepositoryDefinition cmrRepositoryDefinition) {
		this.cmrRepositoryDefinition = cmrRepositoryDefinition;
		HttpInvokerProxyFactoryBean httpInvokerProxyFactoryBean = new HttpInvokerProxyFactoryBean();

		// we need to set the class loader on our own
		// the problems is that the service interface class can not be found
		// I am not quite sure why, but this is suggested on several places as a patch
		httpInvokerProxyFactoryBean.setBeanClassLoader(getClass().getClassLoader());

		// using kryo (de-)serialization for the requests and responses
		KryoSimpleHttpInvokerRequestExecutor kryoSimpleHttpInvokerRequestExecutor = new KryoSimpleHttpInvokerRequestExecutor();
		kryoSimpleHttpInvokerRequestExecutor.setBeanClassLoader(getClass().getClassLoader());
		kryoSimpleHttpInvokerRequestExecutor.setSerializationManagerProvider(serializationManagerProvider);
		httpInvokerProxyFactoryBean.setHttpInvokerRequestExecutor(kryoSimpleHttpInvokerRequestExecutor);

		httpInvokerProxyFactoryBean.setServiceInterface(serviceInterface);
		httpInvokerProxyFactoryBean.setServiceUrl(PROTOCOL + cmrRepositoryDefinition.getIp() + ":" + cmrRepositoryDefinition.getPort() + REMOTING + serviceName);
		httpInvokerProxyFactoryBean.afterPropertiesSet();

		service = httpInvokerProxyFactoryBean.getObject();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CmrRepositoryDefinition getCmrRepositoryDefinition() {
		return cmrRepositoryDefinition;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getService() {
		return service;
	}

	/**
	 * Sets {@link #serviceInterface}.
	 *
	 * @param serviceInterface
	 *            New value for {@link #serviceInterface}
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Gets {@link #serviceInterface}.
	 *
	 * @return {@link #serviceInterface}
	 */
	public Class<?> getServiceInterface() {
		return serviceInterface;
	}

	/**
	 * Sets {@link #serviceName}.
	 *
	 * @param serviceName
	 *            New value for {@link #serviceName}
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Sets {@link #serializationManagerProvider}.
	 *
	 * @param serializationManagerProvider
	 *            New value for {@link #serializationManagerProvider}
	 */
	public void setSerializationManagerProvider(SerializationManagerProvider serializationManagerProvider) {
		this.serializationManagerProvider = serializationManagerProvider;
	}

	/**
	 * Gets {@link #defaultValueOnError}.
	 *
	 * @return {@link #defaultValueOnError}
	 */
	@Override
	public boolean isDefaultValueOnError() {
		return defaultValueOnError;
	}

	/**
	 * Sets {@link #defaultValueOnError}.
	 *
	 * @param defaultValueOnError
	 *            New value for {@link #defaultValueOnError}
	 */
	public void setDefaultValueOnError(boolean defaultValueOnError) {
		this.defaultValueOnError = defaultValueOnError;
	}

}
