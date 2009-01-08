package sernet.springclient;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;

/**
 * The main plugin class to be used in the desktop.
 */
public class SpringClientPlugin extends AbstractUIPlugin {
	private BeanFactory beanFactory;

	private BeanFactoryReference beanFactoryReference;
	
	//The shared instance.
	private static SpringClientPlugin plugin;
	
	/**
	 * The constructor.
	 */
	public SpringClientPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static SpringClientPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("SpringClient", path);
	}
	
	public synchronized void closeBeanFactory() {
		if (beanFactoryReference != null) {
			beanFactoryReference.release();
			beanFactory = null;
		}
	}
	
	public synchronized BeanFactory getBeanFactory() {
		return beanFactory;
	}
	

	public synchronized void openBeanFactory(String beanFactoryUrl, String context) {
		if (beanFactory == null) {
			BeanFactoryLocator beanFactoryLocator = SingletonBeanFactoryLocator.getInstance(beanFactoryUrl);
			beanFactoryReference = beanFactoryLocator.useBeanFactory(context);
			beanFactory = beanFactoryReference.getFactory();
		}
	}	
}
