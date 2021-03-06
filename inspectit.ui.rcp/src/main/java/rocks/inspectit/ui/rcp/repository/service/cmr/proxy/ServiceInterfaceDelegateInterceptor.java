package rocks.inspectit.ui.rcp.repository.service.cmr.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;

import rocks.inspectit.ui.rcp.InspectIT;
import rocks.inspectit.ui.rcp.job.BlockingJob;
import rocks.inspectit.ui.rcp.repository.service.cmr.ICmrService;

/**
 * {@link MethodInterceptor} that delegates the call to the concrete service of a
 * {@link ICmrService} class.
 *
 * @author Ivan Senic
 * @author Marius Oehler
 *
 */
public class ServiceInterfaceDelegateInterceptor implements MethodInterceptor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Object thisObject = methodInvocation.getThis();
		if (thisObject instanceof ICmrService) {
			if (InterceptorUtils.isServiceMethod(methodInvocation)) {
				return invokeMethod(methodInvocation);
			} else {
				return methodInvocation.proceed();
			}
		} else {
			throw new Exception("ServiceInterfaceIntroductionInterceptor not bounded to the ICmrService class.");
		}
	}

	/**
	 * Invoke the given method. See {@link #invoke(MethodInvocation)} for more information.
	 *
	 * @param methodInvocation
	 *            the method invocation joinpoint
	 * @return the result object
	 * @throws Exception
	 *             if the interceptors or the target-object throws an exception
	 */
	private Object invokeMethod(final MethodInvocation methodInvocation) throws Exception {
		final ICmrService cmrService = (ICmrService) methodInvocation.getThis();
		final Object concreteService = cmrService.getService();

		if (checkUiThreadIsUsed(methodInvocation)) {
			BlockingJob<Object> job = new BlockingJob<>("Fetching data from repository..", new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return invokeUsingReflection(concreteService, methodInvocation.getMethod(), methodInvocation.getArguments());
				}
			});

			Object result = job.scheduleAndJoin();

			return result;
		} else {
			// otherwise just execute the call
			Object returnVal = invokeUsingReflection(concreteService, methodInvocation.getMethod(), methodInvocation.getArguments());
			return returnVal;
		}
	}

	/**
	 * Invokes the concrete object using reflection.
	 *
	 * @param concreteService
	 *            Service to invoke.
	 * @param method
	 *            Method to invoke.
	 * @param arguments
	 *            Arguments.
	 * @throws Exception
	 *             If any other exception occurs.
	 * @return Return value.
	 */
	private Object invokeUsingReflection(Object concreteService, Method method, Object[] arguments) throws Exception {
		try {
			return method.invoke(concreteService, arguments);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			if (targetException instanceof Exception) {
				throw (Exception) targetException;
			} else {
				throw new Exception(targetException); // NOPMD
			}
		}
	}

	/**
	 * Checks whether the current thread is the main (UI) thread. If this is true, an exception will
	 * be thrown in development and a log line written in production.
	 *
	 * @param methodInvocation
	 *            the called method
	 * @return Returns <code>true</code> if the current thread is the UI thread
	 */
	private boolean checkUiThreadIsUsed(MethodInvocation methodInvocation) {
		Thread uiThread = Display.getDefault().getThread();
		Thread currentThread = Thread.currentThread();

		if (uiThread.equals(currentThread)) {
			if (InspectIT.getDefault().isDevelopment()) {
				String method = methodInvocation.getMethod().getDeclaringClass() + "." + methodInvocation.getMethod().getName();
				String message = "A service method has been called in the UI thread. Please ensure that service methods are not called in the UI thread. Called service: " + method;

				InspectIT.getDefault().log(IStatus.WARNING, message);
			}
			return true;
		} else {
			return false;
		}
	}
}
