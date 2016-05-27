/*******************************************************************************
 * Copyright (c) 2016 Sebastian Hagedorn.
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Sebastian Hagedorn <sh[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.report.service.impl.security;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.IEngineTask;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.osgi.util.NLS;

import sernet.verinice.security.report.ReportSecurityException;

/**
 * this thread passes a prepared {@link IRunAndRenderTask} (representing a verinice-report)
 * to the BIRT-Report-Engine and executes it from within a secured context. That
 * prevents executing unauthorized code via beanshell or javascript (executed via the rhino-engine)
 * code.
 * @author Sebastian Hagedorn <sh[at]sernet[dot]de>
 */
public class ReportExecutionThread extends Thread {
    
    private static final Logger LOG = Logger.getLogger(ReportExecutionThread.class);
    
    private ReportSecurityManager reportSecurityManager ;
    private IRunAndRenderTask task;
    
    public ReportExecutionThread(IRunAndRenderTask task, ReportSecurityManager secureReportExecutionManager, boolean sandboxEnabled){
        this.task = task;
        this.reportSecurityManager = secureReportExecutionManager;
        reportSecurityManager.setProtectionEnabled(sandboxEnabled);
    }
    
    @Override
    public void run() throws ReportSecurityException{
      SecurityManager old = System.getSecurityManager();
      System.setSecurityManager(reportSecurityManager);
      try{
          runUntrustedCode();
      } catch (ReportSecurityException e){
          throw e;
      } finally {
          // without this, we cannot reset the securityManager, because
          // that needs to be forbidden from within report excecution
          reportSecurityManager.setProtectionEnabled(false);
          System.setSecurityManager(old);
      }
    }
    
    /**
     * note that the so called "untrusted" code is not the line
     * task.run()
     * but the user-generated code, contained in datasets (via beanshell )
     *  or javascript snippets within the template
     */
    private void runUntrustedCode() throws ReportSecurityException{
      try {
          task.setErrorHandlingOption(IEngineTask.CANCEL_ON_ERROR);
          task.run();
          if(!task.getErrors().isEmpty()){
              handleExceptionsFromTask();
          }
      } catch (EngineException t) {
          LOG.error(NLS.bind(Messages.REPORT_RENDER_EXCEPTION_0, task.getReportRunnable().getDesignInstance().getReport().getQualifiedName()), t);
      } catch (ReportSecurityException r){
          /* throw this, to handle it in ui to inform user about prevented
           * execution of unauthorized code  
           */
          throw r;
      } finally {
          task.close();
      }
    }
    


    private void handleExceptionsFromTask() throws ReportSecurityException, EngineException {
        for(Object error : task.getErrors()){
              if(error instanceof EngineException){
                  EngineException ee = (EngineException)error;
                  Throwable rootCause = ExceptionUtils.getRootCause(ee);
                  if(rootCause instanceof ReportSecurityException){
                      throw (ReportSecurityException)rootCause;
                  }
                  throw ee;
              }
          }
    }

}
