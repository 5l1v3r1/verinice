/*******************************************************************************
 * Copyright (c) 2016 Daniel Murygin <dm{a}sernet{dot}de>.
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
 *     Daniel Murygin <dm{a}sernet{dot}de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service.linktable;

import sernet.verinice.model.bsi.IBSIStrukturKategorie;
import sernet.verinice.model.bsi.risikoanalyse.FinishedRiskAnalysis;

/**
 *
 *
 * @author Daniel Murygin <dm{a}sernet{dot}de>
 */
public class RiskAnalysisPropertyAdapter implements IPropertyAdapter<FinishedRiskAnalysis> {

    public RiskAnalysisPropertyAdapter() {
        super();
    }

    /* (non-Javadoc)
     * @see sernet.verinice.service.linktable.IPropertyAdapter#getPropertyValue(java.lang.Object, java.lang.String)
     */
    @Override
    public String getPropertyValue(FinishedRiskAnalysis element, String propertyId) {
        return element.getTitle();
    }

}
