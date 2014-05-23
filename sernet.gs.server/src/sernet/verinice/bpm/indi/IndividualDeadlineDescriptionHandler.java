/*******************************************************************************
 * Copyright (c) 2012 Daniel Murygin.
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
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.bpm.indi;

import java.util.Map;

import sernet.verinice.bpm.TaskService;
import sernet.verinice.interfaces.bpm.IIndividualProcess;
import sernet.verinice.interfaces.bpm.ITaskDescriptionHandler;
import sernet.verinice.model.bpm.Messages;

/**
 *
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class IndividualDeadlineDescriptionHandler extends IndividualTaskDescriptionHandler implements ITaskDescriptionHandler {

    @Override
    public String loadTitle(String taskId, Map<String, Object> varMap) {
        return Messages.getString(getTitleKey());
    }
    
    protected String getDescriptionKey() {
        return IIndividualProcess.TASK_DEADLINE + TaskService.DESCRIPTION_SUFFIX;
    }
    
    protected String getTitleKey() {
        return IIndividualProcess.TASK_DEADLINE;
    }

}
