/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Alexander Koderman <ak[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.ui.rcp.gsimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.exception.SQLGrammarException;

import sernet.gs.model.Baustein;
import sernet.gs.reveng.MSchutzbedarfkategTxt;
import sernet.gs.reveng.MbBaust;
import sernet.gs.reveng.MbBaustGefaehr;
import sernet.gs.reveng.MbMassn;
import sernet.gs.reveng.MbZeiteinheitenTxt;
import sernet.gs.reveng.ModZobjBst;
import sernet.gs.reveng.ModZobjBstMass;
import sernet.gs.reveng.NZielobjekt;
import sernet.gs.reveng.NZobSb;
import sernet.gs.reveng.importData.BausteinInformationTransfer;
import sernet.gs.reveng.importData.BausteineMassnahmenResult;
import sernet.gs.reveng.importData.ESAResult;
import sernet.gs.reveng.importData.GefaehrdungInformationTransfer;
import sernet.gs.reveng.importData.MassnahmeInformationTransfer;
import sernet.gs.reveng.importData.ZielobjektTypeResult;
import sernet.gs.service.GSServiceException;
import sernet.gs.service.TimeFormatter;
import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.CnAWorkspace;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.bsi.model.BSIConfigurationRCPLocal;
import sernet.gs.ui.rcp.main.bsi.model.BSIMassnahmenModel;
import sernet.gs.ui.rcp.main.bsi.model.CnAElementBuilder;
import sernet.gs.ui.rcp.main.bsi.model.GSScraperUtil;
import sernet.gs.ui.rcp.main.bsi.model.IBSIConfig;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.CnAElementHome;
import sernet.gs.ui.rcp.main.common.model.CnATreeElementBuildException;
import sernet.gs.ui.rcp.main.preferences.PreferenceConstants;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.gs.ui.rcp.main.service.grundschutzparser.LoadBausteine;
import sernet.gs.ui.rcp.main.service.taskcommands.ImportCreateBausteinReferences2;
import sernet.gs.ui.rcp.main.service.taskcommands.ImportCreateBausteine;
import sernet.gs.ui.rcp.main.service.taskcommands.ImportIndividualMassnahmen;
import sernet.gs.ui.rcp.main.service.taskcommands.ImportTransferSchutzbedarf;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.model.bsi.BausteinUmsetzung;
import sernet.verinice.model.bsi.ITVerbund;
import sernet.verinice.model.bsi.MassnahmenUmsetzung;
import sernet.verinice.model.bsi.NetzKomponente;
import sernet.verinice.model.bsi.Person;
import sernet.verinice.model.bsi.Schutzbedarf;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.Link;

import com.heatonresearch.datamover.DataMover;
import com.heatonresearch.datamover.db.Database;
import com.heatonresearch.datamover.db.DerbyDatabase;
import com.heatonresearch.datamover.db.MDBFileDatabase;

/**
 * Import GSTOOL(tm) databases using the GSVampire. Maps GStool-database objects
 * to Verinice-Objects and fields.
 *
 * @author koderman[at]sernet[dot]de
 * @version $Rev$ $LastChangedDate$ $LastChangedBy$
 *
 */
public class ImportTask extends AbstractGstoolImportTask {

    private static final Logger LOG = Logger.getLogger(ImportTask.class);

    private IProgress monitor;
    int numberOfElements;
    int numberImported;
    private TransferData transferData;

    private List<MbZeiteinheitenTxt> zeiten;
    private final Map<ModZobjBstMass, MassnahmenUmsetzung> alleMassnahmen;
    private final Map<NZielobjekt, CnATreeElement> alleZielobjekte = new HashMap<NZielobjekt, CnATreeElement>();
    private final List<Person> allePersonen = new ArrayList<Person>();

    private List<Baustein> allCatalogueBausteine;

    // map of zielobjekt-guid to itverbund NZielobjekt:
    private final Map<String, NZielobjekt> itverbundZuordnung = new HashMap<String, NZielobjekt>();

    private final boolean importBausteine;
    private final boolean massnahmenPersonen;
    private final boolean bausteinPersonen;
    private final boolean zielObjekteZielobjekte;
    private final boolean schutzbedarf;
    private final boolean importRollen;
    private final boolean kosten;
    private final boolean importUmsetzung;

    private final Map<MbBaust, BausteinUmsetzung> alleBausteineToBausteinUmsetzungMap;
    private final Map<MbBaust, ModZobjBst> alleBausteineToZoBstMap;
    private final Map<MbBaust, Baustein> gstool2VeriniceBausteinMap;
    private final Map<BausteinUmsetzung, List<BausteineMassnahmenResult>> individualMassnahmenMap;  
    private final Map<NZielobjekt, List<BausteineMassnahmenResult>> nZielObjektBausteineMassnahmenResultMap;
   
    private String sourceId;

    public ImportTask(boolean bausteine, boolean massnahmenPersonen, boolean zielObjekteZielobjekte, boolean schutzbedarf, boolean importRollen, boolean kosten, boolean umsetzung, boolean bausteinPersonen) {
        this.importBausteine = bausteine;
        this.massnahmenPersonen = massnahmenPersonen;
        this.zielObjekteZielobjekte = zielObjekteZielobjekte;
        this.schutzbedarf = schutzbedarf;
        this.importRollen = importRollen;
        this.kosten = kosten;
        this.importUmsetzung = umsetzung;
        this.bausteinPersonen = bausteinPersonen;

        this.alleBausteineToBausteinUmsetzungMap = new HashMap<>();
        this.alleBausteineToZoBstMap = new HashMap<>();
        this.alleMassnahmen = new HashMap<>();
        this.gstool2VeriniceBausteinMap = new HashMap<>();
        this.individualMassnahmenMap = new HashMap<>();
        this.nZielObjektBausteineMassnahmenResultMap = new HashMap<>();
    }

    protected void executeTask(int importType, IProgress monitor) throws Exception {     
        Preferences prefs = Activator.getDefault().getPluginPreferences();
        String sourceDbUrl = prefs.getString(PreferenceConstants.GS_DB_URL);
        if (sourceDbUrl.indexOf("odbc") > -1) {
            copyMDBToTempDB(sourceDbUrl);
        }
        this.allCatalogueBausteine = loadCatalogueBausteine();

        this.monitor = monitor;

        zeiten = getGstoolDao().findZeiteinheitenTxtAll();

        // print all types and subtypes to debug, in case we need to add
        // those to our mapping manually;
        List<ZielobjektTypeResult> findZielobjektTypAll = getGstoolDao().findZielobjektTypAll();
        LOG.debug("List of all ZO types in GSTOOL DB: ");
        for (ZielobjektTypeResult zielobjektTypeResult : findZielobjektTypAll) {
            LOG.debug(zielobjektTypeResult.subtype + "=" + zielobjektTypeResult.type);
        }

        transferData = new TransferData(getGstoolDao(), importRollen);
        this.sourceId = importZielobjekte();
    }

    /**
     * Imports all zielobjekte from GSTOOL with all selected properties. Returns
     * the sourceID with which all objects have been created.
     *
     * @return
     * @throws Exception
     */
    private String importZielobjekte() throws Exception {
        // generate a sourceId for objects created by this import:
        final int maxUuidLength = 6;
        final String defaultSubTaskDescription = "Die Daten werden gespeichert.";
        String sourceId = UUID.randomUUID().toString().substring(0, maxUuidLength);

        List<ZielobjektTypeResult> zielobjekte = findZielobjekte();       
            
        numberOfElements = zielobjekte.size();
        numberImported = 0;
        if (this.importBausteine) {
            monitor.beginTask("Importiere Zielobjekte, Bausteine und Massnahmen...", numberOfElements);
        } else {
            monitor.beginTask("Importiere Zielobjekte...", numberOfElements);
        }

        // create special ITVerbund for elements that are not linked to an
        // ItVerbund in GSTOOL
        // these can exist and may have links to other objects that ARE linked
        // to an ITVerbund, so we have to put them somewhere

        ITVerbund itverbundForOrphans = null;

        // create all found ITVerbund first
        List<ITVerbund> neueVerbuende = importItVerbuende(sourceId, zielobjekte);
        
        long startTime = System.currentTimeMillis();
        // create all Zielobjekte in their respective ITVerbund,
        for (ZielobjektTypeResult zielobjekt : zielobjekte) {
            String typeId = GstoolTypeMapper.getVeriniceTypeOrDefault(zielobjekt.type, zielobjekt.subtype);
            if(LOG.isDebugEnabled()){
                LOG.debug("GSTOOL type id " + zielobjekt.type + " : " + zielobjekt.subtype + " was translated to: " + typeId);
            }
            if (typeId.equals(ITVerbund.TYPE_ID)) {
                continue;
            }
            CnATreeElement element = null;
            if (neueVerbuende.size() > 0) {
                // find correct itverbund for resultZO
                NZielobjekt origITVerbundZO = itverbundZuordnung.get(zielobjekt.zielobjekt.getGuid());
                ITVerbund itverbund = (ITVerbund) alleZielobjekte.get(origITVerbundZO);
                if (itverbund == null) {
                    LOG.debug("ITVerbund not found for ZO: " + zielobjekt.zielobjekt.getName() + ". Created in BSI");
                    if (itverbundForOrphans == null) {
                        itverbundForOrphans = (ITVerbund) CnAElementFactory.getInstance().saveNew(CnAElementFactory.getLoadedModel(), ITVerbund.TYPE_ID, null, false);
                        itverbundForOrphans.setTitel("---Waisenhaus: Zielobjekte ohne IT-Verbund-Zuordnung");
                        itverbundForOrphans.setSourceId(sourceId);
                        CnAElementHome.getInstance().update(itverbundForOrphans);
                    }
                    itverbund = itverbundForOrphans;
                }
                LOG.debug("Creating ZO " + zielobjekt.zielobjekt.getName() + " in ITVerbund " + itverbund.getTitle());
                itverbund.setSourceId(sourceId);
                element = CnAElementBuilder.getInstance().buildAndSave(itverbund, typeId);
            }
            if (element != null) {
                // save element for later:
                alleZielobjekte.put(zielobjekt.zielobjekt, element);

                // separately save persons:
                if (element instanceof Person) {
                    allePersonen.add((Person) element);
                }

                transferData.transfer(element, zielobjekt);
                element = importEsa(zielobjekt, element);
                element.setSourceId(sourceId);
                monitor.subTask(numberImported + "/" + numberOfElements + " - " + element.getTitle());
                
                createBausteine(sourceId, element, zielobjekt.zielobjekt);
                
                CnAElementHome.getInstance().update(element);
                
                monitor.worked(1);
                numberImported++;
            }

        }

        long durationImportZO = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        // create inidividual massnahmen
        createIndividualMassnahmen();
        long durationImportIndividual = System.currentTimeMillis() - startTime;

        monitor.subTask(defaultSubTaskDescription);

        startTime = System.currentTimeMillis();
        importMassnahmenVerknuepfungen();
        long durationImportMnLinks = System.currentTimeMillis() - startTime;
        monitor.subTask(defaultSubTaskDescription);

        // update this.alleMassnahmen
        Collection<MassnahmenUmsetzung> allMnUms = this.alleMassnahmen.values();
        allMnUms.removeAll(Collections.singleton(null));
        ArrayList<CnATreeElement> toUpdate = new ArrayList<CnATreeElement>();
        toUpdate.addAll(allMnUms);
        LOG.debug("Saving person links to measures.");
        monitor.beginTask("Verknüpfe Ansprechpartner mit Massnahmen...", toUpdate.size());
        ElementListUpdater updater = new ElementListUpdater(toUpdate, monitor);
        updater.setMaxNumberPerCommand(500);
        updater.execute();

        startTime = System.currentTimeMillis();
        importBausteinPersonVerknuepfungen();
        long durationImportBstPrsnLinks = System.currentTimeMillis() - startTime;
        monitor.subTask(defaultSubTaskDescription);

        // update this. alleBausteineToBausteinUmsetzungMap
        toUpdate = new ArrayList<CnATreeElement>();
        toUpdate.addAll(this.alleBausteineToBausteinUmsetzungMap.values());
        if(LOG.isDebugEnabled()) {
            LOG.debug("Saving person links to modules.");
        }
        monitor.beginTask("Verknüpfe Ansprechpartner mit Bausteinen...", toUpdate.size());
        updater = new ElementListUpdater(toUpdate, monitor);
        updater.setMaxNumberPerCommand(500);
        updater.execute();

        startTime = System.currentTimeMillis();
        importZielobjektVerknuepfungen();
        long durationImportZoLinks = System.currentTimeMillis() - startTime;
        monitor.subTask(defaultSubTaskDescription);

        startTime = System.currentTimeMillis();
        importSchutzbedarf();
        long durationImportSB = System.currentTimeMillis() - startTime;
        monitor.subTask(defaultSubTaskDescription);

        int n = zielobjekte.size();
        monitor.beginTask("Lese Bausteinreferenzen...", n);
        int i = 1;
        startTime = System.currentTimeMillis();
        for (NZielobjekt zielobjekt : alleZielobjekte.keySet()) {
            CnATreeElement element = alleZielobjekte.get(zielobjekt);
            monitor.subTask(i + "/" + n + " - " + element.getTitle());
            createBausteinReferences(sourceId, element, zielobjekt);
            monitor.worked(1);
            i++;
        }
        long durationImportBstRef = System.currentTimeMillis() - startTime;
        monitor.done();

        if(LOG.isDebugEnabled()){
            LOG.debug("Duration of importing zielobjekte:\t" + TimeFormatter.getHumanRedableTime(durationImportZO));
            LOG.debug("Duration of importing individual massnahmen:\t" + TimeFormatter.getHumanRedableTime(durationImportIndividual));
            LOG.debug("Duration of importing massnahmen links:\t" + TimeFormatter.getHumanRedableTime(durationImportMnLinks));
            LOG.debug("Duration of importing links between bausteine and persons:\t" + TimeFormatter.getHumanRedableTime(durationImportBstPrsnLinks));
            LOG.debug("Duration of importing links between zielobjekte:\t" + TimeFormatter.getHumanRedableTime(durationImportZoLinks));
            LOG.debug("Duration of importing schutzbedarf:\t" + TimeFormatter.getHumanRedableTime(durationImportSB));
            LOG.debug("Duration of importing links between zielobjekte and bausteine:\t" + TimeFormatter.getHumanRedableTime(durationImportBstRef));
        }

        return sourceId;
    }

    private List<ITVerbund> importItVerbuende(String sourceId, List<ZielobjektTypeResult> zielobjekte) throws CommandException, CnATreeElementBuildException {
        List<ITVerbund> neueVerbuende = new ArrayList<ITVerbund>();
        for (ZielobjektTypeResult zielobjekt : zielobjekte) {         
            if (ITVerbund.TYPE_ID.equals(GstoolTypeMapper.getVeriniceTypeOrDefault(zielobjekt.type, zielobjekt.subtype))) {
                ITVerbund itverbund = (ITVerbund) CnAElementFactory.getInstance().saveNew(CnAElementFactory.getLoadedModel(), ITVerbund.TYPE_ID, null, false);
                itverbund.setSourceId(sourceId);
                neueVerbuende.add(itverbund);
                monitor.worked(1);
                numberImported++;

                // save element for later:
                alleZielobjekte.put(zielobjekt.zielobjekt, itverbund);

                transferData.transfer(itverbund, zielobjekt);
                createBausteine(sourceId, itverbund, zielobjekt.zielobjekt);

                // save links from itverbuende to other objects to
                // facilitate creating ZOs in their correct IT-Verbund:
                List<NZielobjekt> itvLinks = getGstoolDao().findLinksByZielobjekt(zielobjekt.zielobjekt);
                for (NZielobjekt nZielobjekt : itvLinks) {
                    LOG.debug("Saving Zuordnung from ZO" + nZielobjekt.getName() + "(GUID " + nZielobjekt.getGuid() + ") to ITVerbund " + zielobjekt.zielobjekt.getName());
                    itverbundZuordnung.put(nZielobjekt.getGuid(), zielobjekt.zielobjekt);
                }
            }      
        }
        return neueVerbuende;
    }

    private Collection<MassnahmenUmsetzung> createIndividualMassnahmen() throws CommandException {
        // does not work anymore, check why
        Map<String, MassnahmeInformationTransfer> massnahmenInfos = new HashMap<>();
        for(List<BausteineMassnahmenResult> bausteineMassnahmenResultList : individualMassnahmenMap.values()){
            massnahmenInfos.putAll(createIndividualMassnahmenForBausteineMassnahmenResult(bausteineMassnahmenResultList));
        }
        ImportIndividualMassnahmen command = new ImportIndividualMassnahmen(individualMassnahmenMap, alleMassnahmen, allCatalogueBausteine, massnahmenInfos);
        command = ServiceFactory.lookupCommandService().executeCommand(command);
        CnAElementHome.getInstance().update(new ArrayList<MassnahmenUmsetzung>(command.getChangedElements()));
        return command.getChangedElements();
    }

    private Map<String, MassnahmeInformationTransfer> createIndividualMassnahmenForBausteineMassnahmenResult(List<BausteineMassnahmenResult> bausteineMassnahmenResultList) {
        Map<String, MassnahmeInformationTransfer> massnahmenInfos = new HashMap<>();
        for(BausteineMassnahmenResult bausteineMassnahmenResult : bausteineMassnahmenResultList){
            if(!massnahmenInfos.containsKey(bausteineMassnahmenResult)){
                MassnahmeInformationTransfer massnahmeInformationTransfer = getGstoolDao().
                        findTxtforMbMassn(bausteineMassnahmenResult.baustein, bausteineMassnahmenResult.massnahme,
                                GSScraperUtil.getInstance().getModel().getEncoding());
                massnahmenInfos.put(TransferData.createBausteineMassnahmenResultIdentifier(bausteineMassnahmenResult), massnahmeInformationTransfer);
            }
        }
        return massnahmenInfos;
    }


    private List<ZielobjektTypeResult> findZielobjekte() throws Exception {
        List<ZielobjektTypeResult> zielobjekte;
        try {
            zielobjekte = getGstoolDao().findZielobjektTypAll();
        } catch (SQLGrammarException e) {
            SQLGrammarException sqlException = e;
            // wrong db version has columns missing, i.e. "GEF_ID":
            if (sqlException.getSQLException().getMessage().indexOf("GEF_OK") > -1) {
                ExceptionUtil.log(sqlException.getSQLException(), "Fehler beim Laden der Zielobjekte. Möglicherweise falsche Datenbankversion des GSTOOL? " + "\nEs wird nur der Import der aktuellen Version (4.7) des GSTOOL unterstützt.");
            }
            throw e;
        } catch (Exception e) {
            ExceptionUtil.log(e, "Fehler beim Laden der Zielobjekte");
            throw e;
        }
        return zielobjekte;
    }
    
    Set<String> createdLinks = new HashSet();
    
    /**
     * @param sourceId
     * @param element
     * @param zielobjekt
     * @throws CommandException
     */
    private void createBausteinReferences(String sourceId, CnATreeElement element, NZielobjekt zielobjekt) throws CommandException {
        if (!importBausteine) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        List<BausteineMassnahmenResult> findBausteinMassnahmenByZielobjekt = null;
        if(nZielObjektBausteineMassnahmenResultMap.containsKey(zielobjekt)) {
            findBausteinMassnahmenByZielobjekt = nZielObjektBausteineMassnahmenResultMap.get(zielobjekt);
        } else {
            findBausteinMassnahmenByZielobjekt = Collections.EMPTY_LIST;
        }
        
        findBausteinMassnahmenByZielobjekt = reduceBausteinMassnahmeResultToOnePerBaustein(findBausteinMassnahmenByZielobjekt);
        
        for(BausteineMassnahmenResult bausteineMassnahmenResult : findBausteinMassnahmenByZielobjekt) {
            Set<CnATreeElement> targets = new HashSet<>();
            List<Integer> targetIds = getGstoolDao().findReferencedZobsByBaustein(bausteineMassnahmenResult.zoBst, zielobjekt.getId().getZobId());
            for(Integer targetId : targetIds) {
                CnATreeElement mappedElement = getCnATreeElementByZobId(targetId);
                if(mappedElement != null) {
                    targets.add(mappedElement);
                }
            }
            
            
            BausteinUmsetzung bu = getVeriniceBausteinUmsetzung(bausteineMassnahmenResult, element);
            Set<CnATreeElement> filteredTargets = new HashSet<>();
            StringBuilder sb = new StringBuilder();
            if(bu != null){
                for(CnATreeElement target : targets) {
                    sb.append(bu.hashCode()).append("#").append(target.hashCode());
                    if(!createdLinks.contains(sb.toString())) {
                        filteredTargets.add(target);
                    }
                    sb.setLength(0);
                }
                ImportCreateBausteinReferences2 command = new ImportCreateBausteinReferences2(bu, filteredTargets);
                long commandStart = System.currentTimeMillis();
                command = ServiceFactory.lookupCommandService().executeCommand(command);
                createdLinks.addAll(command.getCreatedLinksIdentifier());
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Time executing command on element <" + element.getTitle() + ">:\t" + String.valueOf( ((System.currentTimeMillis() - commandStart) / 1000) ) + "s");
                }
            } else {
                LOG.warn("BausteinUmsetzung with Nr.:" + bausteineMassnahmenResult.baustein.getNr() +  " not found for element "  + element.getTitle());
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("Time computing references for element <" + element.getTitle() + ">:\t" + String.valueOf( ((System.currentTimeMillis() - startTime) / 1000) ) + "s");
        }
        
//        Map<MbBaust, List<BausteineMassnahmenResult>> bausteineMassnahmenMap = transferData.convertBausteinMap(findBausteinMassnahmenByZielobjekt);
//        
//        ImportCreateBausteinReferences command;
//        command = new ImportCreateBausteinReferences(sourceId, element, bausteineMassnahmenMap, alleBausteineToZoBstMap, gstool2VeriniceBausteinMap, this.allCatalogueBausteine);
//        ServiceFactory.lookupCommandService().executeCommand(command);
    }
    
    private List<BausteineMassnahmenResult> reduceBausteinMassnahmeResultToOnePerBaustein(List<BausteineMassnahmenResult> inputList){
        long startTime = System.currentTimeMillis();
        Map<Integer, BausteineMassnahmenResult> map = new HashMap<>();
        for(BausteineMassnahmenResult bausteineMassnahmenResult : inputList) {
            if(!map.containsKey(bausteineMassnahmenResult.baustein.getId().getBauId())) {
                map.put(bausteineMassnahmenResult.baustein.getId().getBauId(), bausteineMassnahmenResult);
            }
        }
        List<BausteineMassnahmenResult> reducedList = new ArrayList<>(map.size());
        reducedList.addAll(map.values());
        if(LOG.isDebugEnabled()) {
            LOG.debug("Reduced BausteineMassnahmenList from " + inputList.size() + " to " + reducedList.size());
            LOG.debug("Time needed for Reducing:\t" + String.valueOf( ((System.currentTimeMillis() - startTime) / 1000) ) + "s");
        }
        return reducedList;
    }
    
    private boolean mbBausteinEquals(MbBaust mbB1, MbBaust mbB2){
        return mbB1.getNr().equals(mbB2.getNr()) && mbB1.getId().getBauId().equals(mbB2.getId().getBauId());
    }

    private BausteinUmsetzung getVeriniceBausteinUmsetzung(BausteineMassnahmenResult bausteineMassnahmeResult, CnATreeElement parent){
        if(parent.getTitle().equals("Druckservice Bad Godesberg") && bausteineMassnahmeResult.baustein.getNr().equals("5.99")){
            this.hashCode();
        }
        for(MbBaust mbBKey : alleBausteineToBausteinUmsetzungMap.keySet()){
            if(mbBausteinEquals(mbBKey, bausteineMassnahmeResult.baustein)){
                BausteinUmsetzung bausteinUmsetzung = alleBausteineToBausteinUmsetzungMap.get(mbBKey);
                if(parent.equals(bausteinUmsetzung.getParent())){
                    return bausteinUmsetzung;
                }
                
//                for(Entry<NZielobjekt, CnATreeElement> entry : alleZielobjekte.entrySet()) {
//                    if(veriniceObject.getTitle().equals("Internet-Recherche") && entry.getValue().getTitle().equals("Internet-Recherche") &&
//                            entry.getKey().getId().getZobId().equals(bausteineMassnahmeResult.zoBst.getId().getZobId())){
//                        this.hashCode();
//                    }
//                    if(entry.getKey().getId().getZobId().equals(bausteineMassnahmeResult.zoBst.getRefZobId())){
//                        this.hashCode();
//                    }
//                    if(entry.getValue().equals(veriniceObject)) {
//                        NZielobjekt nZielobjekt = entry.getKey();
//                        if(nZielobjekt.getId().getZobId().equals(bausteineMassnahmeResult.zoBst.getId().getZobId())) {
//                            return bausteinUmsetzung;
//                        } else {
//                            break;
//                        }
//                    }
//                }
            }
        }
        return null;
    }
    
    private CnATreeElement getCnATreeElementByZobId(Integer zobId) {
        for(NZielobjekt zielobjekt : alleZielobjekte.keySet()) {
            if(zobId.equals(zielobjekt.getId().getZobId())){
                return alleZielobjekte.get(zielobjekt);
            }
        }
        return null;
    }

    private void importSchutzbedarf() throws Exception {
        if (!schutzbedarf) {
            return;
        }

        monitor.beginTask("Importiere Schutzbedarf für alle Zielobjekte...", alleZielobjekte.size());
        Set<Entry<NZielobjekt, CnATreeElement>> alleZielobjekteEntries = alleZielobjekte.entrySet();

        for (Entry<NZielobjekt, CnATreeElement> entry : alleZielobjekteEntries) {
            List<NZobSb> internalSchutzbedarf = getGstoolDao().findSchutzbedarfByZielobjekt(entry.getKey());
            for (NZobSb schubeda : internalSchutzbedarf) {
                CnATreeElement element = entry.getValue();

                MSchutzbedarfkategTxt vertr = getGstoolDao().findSchutzbedarfNameForId(schubeda.getZsbVertrSbkId());
                MSchutzbedarfkategTxt verfu = getGstoolDao().findSchutzbedarfNameForId(schubeda.getZsbVerfuSbkId());
                MSchutzbedarfkategTxt integ = getGstoolDao().findSchutzbedarfNameForId(schubeda.getZsbIntegSbkId());

                int vertraulichkeit = (vertr != null) ? transferData.translateSchutzbedarf(vertr.getName()) : Schutzbedarf.UNDEF;

                int verfuegbarkeit = (verfu != null) ? transferData.translateSchutzbedarf(verfu.getName()) : Schutzbedarf.UNDEF;

                int integritaet = (integ != null) ? transferData.translateSchutzbedarf(integ.getName()) : Schutzbedarf.UNDEF;

                String vertrBegruendung = schubeda.getZsbVertrBegr();
                String verfuBegruendung = schubeda.getZsbVerfuBegr();
                String integBegruendung = schubeda.getZsbIntegBegr();

                Short isPersonenbezogen = schubeda.getZsbPersDaten();
                if (isPersonenbezogen == null) {
                    isPersonenbezogen = 0;
                }

                ImportTransferSchutzbedarf command = new ImportTransferSchutzbedarf(element, vertraulichkeit, verfuegbarkeit, integritaet, vertrBegruendung, verfuBegruendung, integBegruendung, isPersonenbezogen);
                command = ServiceFactory.lookupCommandService().executeCommand(command);
            }
        }

    }

    private void importZielobjektVerknuepfungen() throws CommandException {
        if (!this.zielObjekteZielobjekte) {
            return;
        }
        Set<NZielobjekt> allElements = alleZielobjekte.keySet();
        List<Link> linkList = new LinkedList<Link>();
        for (NZielobjekt zielobjekt : allElements) {
            monitor.worked(1);
            CnATreeElement dependant = alleZielobjekte.get(zielobjekt);
            List<NZielobjekt> dependencies = getGstoolDao().findLinksByZielobjekt(zielobjekt);

            for (NZielobjekt dependency : dependencies) {
                CnATreeElement dependencyElement = findZielobjektFor(dependency);
                if (dependencyElement == null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Kein Ziel gefunden für Verknüpfung von " + dependant.getTitle() + " zu ZO: " + dependency.getName());
                    }
                    continue;
                }
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Neue Verknüpfung von " + dependant.getTitle() + " zu " + dependencyElement.getTitle());
                }
                // verinice models dependencies DOWN, not UP as the gstool.
                // therefore we need to turn things around, except for persons,
                // networks and itverbund
                // (look at it in the tree and it will make sense):
                CnATreeElement from;
                CnATreeElement to;
                if (dependencyElement instanceof Person || dependencyElement instanceof NetzKomponente || dependant instanceof ITVerbund) {
                    from = dependant;
                    to = dependencyElement;
                } else {
                    from = dependencyElement;
                    to = dependant;
                }
                linkList.add(new Link(from, to));
            }
            ;
        }
        LinkCreater linkCreater = new LinkCreater(linkList, monitor);
        int n = linkList.size();
        monitor.beginTask("Importiere Verknüpfungen von Zielobjekten...", n);
        linkCreater.execute();
    }

    private CnATreeElement findZielobjektFor(NZielobjekt dependency) {
        for (NZielobjekt zielobjekt : alleZielobjekte.keySet()) {
            if (zielobjekt.getId().equals(dependency.getId())) {
                return alleZielobjekte.get(zielobjekt);
            }
        }
        return null;
    }

    private void importMassnahmenVerknuepfungen() {
        if (!this.massnahmenPersonen || !this.importBausteine) {
            return;
        }

        monitor.beginTask("Verknüpfe Ansprechpartner mit Massnahmen...", alleMassnahmen.size());
        int n = alleMassnahmen.keySet().size();
        int current = 1;
        for (ModZobjBstMass obm : alleMassnahmen.keySet()) {
            monitor.worked(1);
            monitor.subTask(current + "/" + n + " - " + alleMassnahmen.get(obm).getTitle());
            current++;
            // transferiere individuell verknüpfte verantowrtliche in massnahmen
            // (TAB "Verantwortlich" im GSTOOL):
            Set<NZielobjekt> personenSrc = getGstoolDao().findVerantowrtlicheMitarbeiterForMassnahme(obm.getId());
            if (personenSrc != null && personenSrc.size() > 0) {
                List<Person> dependencies = findPersonen(personenSrc);
                if (dependencies.size() != personenSrc.size()) {
                    LOG.warn("ACHTUNG: Es wurde mindestens eine Person für die zu verknüpfenden Verantwortlichen nicht gefunden.");
                }
                MassnahmenUmsetzung dependantMassnahme = alleMassnahmen.get(obm);
                for (Person personToLink : dependencies) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Verknüpfe Massnahme " + dependantMassnahme.getTitle() + " mit Person " + personToLink.getTitle());
                    }
                    dependantMassnahme.addUmsetzungDurch(personToLink);
                }
            }
        }
    }

    private Baustein findBausteinForId(String id) {
        for (Baustein baustein : allCatalogueBausteine) {
            if (baustein.getId().equals(id)) {
                return baustein;
            }
        }
        return null;
    }



    private void importBausteinPersonVerknuepfungen() {
        Set<BausteinUmsetzung> changedElements = new HashSet<>();
        if (!this.bausteinPersonen || !this.importBausteine) {
            return;
        }

        monitor.beginTask("Verknüpfe Personen mit Bausteinen...", alleBausteineToBausteinUmsetzungMap.size());
        Set<MbBaust> keySet = alleBausteineToBausteinUmsetzungMap.keySet();
        for (MbBaust mbBaust : keySet) {
            monitor.worked(1);
            BausteinUmsetzung bausteinUmsetzung = alleBausteineToBausteinUmsetzungMap.get(mbBaust);
            if (bausteinUmsetzung != null) {

                NZielobjekt interviewer = alleBausteineToZoBstMap.get(mbBaust).getNZielobjektByFkZbZ2();
                if (interviewer != null) {
                    HashSet<NZielobjekt> set = new HashSet<NZielobjekt>();
                    set.add(interviewer);
                    List<Person> personen = findPersonen(set);
                    if (personen != null && personen.size() > 0) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Befragung für Baustein " + bausteinUmsetzung.getTitle() + " durchgeführt von " + personen.get(0));
                        }
                        bausteinUmsetzung.addBefragungDurch(personen.get(0));
                        changedElements.add(bausteinUmsetzung);
                    }
                }

                Set<NZielobjekt> befragteMitarbeiter = getGstoolDao().findBefragteMitarbeiterForBaustein(alleBausteineToZoBstMap.get(mbBaust).getId());
                if (befragteMitarbeiter != null && befragteMitarbeiter.size() > 0) {
                    List<Person> dependencies = findPersonen(befragteMitarbeiter);
                    if (dependencies.size() != befragteMitarbeiter.size()) {
                        LOG.warn("ACHTUNG: Es wurde mindestens eine Person für die " + "zu verknüpfenden Interviewpartner nicht gefunden.");
                    }
                    monitor.subTask(bausteinUmsetzung.getTitle());
                    for (Person personToLink : dependencies) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Verknüpfe Baustein " + bausteinUmsetzung.getTitle() + " mit befragter Person " + personToLink.getTitle());
                        }
                        bausteinUmsetzung.addBefragtePersonDurch(personToLink);
                        changedElements.add(bausteinUmsetzung);
                    }
                }
            }
        }
    }

    private List<Person> findPersonen(Set<NZielobjekt> personen) {
        List<Person> result = new ArrayList<Person>();
        alleZielobjekte: for (NZielobjekt nzielobjekt : personen) {
            for (Person person : allePersonen) {
                if (person.getKuerzel().equals(nzielobjekt.getKuerzel()) && person.getErlaeuterung().equals(nzielobjekt.getBeschreibung())) {
                    result.add(person);
                    continue alleZielobjekte;
                }
            }
        }
        return result;
    }

    private CnATreeElement createBausteine(String sourceId, CnATreeElement element, NZielobjekt zielobjekt) throws CommandException {
        if (!importBausteine) {
            return element;
        }

        List<BausteineMassnahmenResult> findBausteinMassnahmenByZielobjekt = getGstoolDao().findBausteinMassnahmenByZielobjekt(zielobjekt);
        nZielObjektBausteineMassnahmenResultMap.put(zielobjekt, findBausteinMassnahmenByZielobjekt);

        Map<MbBaust, List<BausteineMassnahmenResult>> bausteineMassnahmenMap = transferData.convertBausteinMap(findBausteinMassnahmenByZielobjekt);

        this.monitor.subTask(numberImported + "/" + numberOfElements + " - Erstelle " + zielobjekt.getName() + " mit " + bausteineMassnahmenMap.keySet().size() + " Baust. und " + getAnzahlMassnahmen(bausteineMassnahmenMap) + " Massn...");

        // maps needed for import of userdefined data, storing information retrieved from itgs catalogues in non userdefined case
        Map<MbBaust, BausteinInformationTransfer> udBausteineTxtMap = new HashMap<>();
        Map<MbMassn, MassnahmeInformationTransfer> udBstMassTxtMap = new HashMap<>();
        Map<MbBaust, List<GefaehrdungInformationTransfer>> udBaustGefMap = new HashMap<>();

        ImportCreateBausteine command;
        ServiceFactory.lookupAuthService();
        for(MbBaust b : bausteineMassnahmenMap.keySet()){
            if(b.getId().getBauImpId() == 1){// is it possible for a catalog bst to have user defined gefs?
                prepareUserDefinedBausteinImport(zielobjekt, bausteineMassnahmenMap, udBausteineTxtMap, udBstMassTxtMap, udBaustGefMap, b);
            }
        }
        command = new ImportCreateBausteine(sourceId, element, bausteineMassnahmenMap, zeiten, kosten, importUmsetzung, udBausteineTxtMap, udBstMassTxtMap, udBaustGefMap, allCatalogueBausteine);
        command = ServiceFactory.lookupCommandService().executeCommand(command);

        if (command.getAlleBausteineToBausteinUmsetzungMap() != null) {
            this.alleBausteineToBausteinUmsetzungMap.putAll(command.getAlleBausteineToBausteinUmsetzungMap());
        }

        if (command.getAlleBausteineToZoBstMap() != null) {
            this.alleBausteineToZoBstMap.putAll(command.getAlleBausteineToZoBstMap());
        }

        if (command.getAlleMassnahmen() != null) {
            this.alleMassnahmen.putAll(command.getAlleMassnahmen());
        }

        if(command.getGstool2VeriniceBausteinMap() != null && command.getGstool2VeriniceBausteinMap().size() > 0){
            this.gstool2VeriniceBausteinMap.putAll(command.getGstool2VeriniceBausteinMap());
        }

        if(command.getIndividualMassnahmenMap() != null && command.getIndividualMassnahmenMap().size() > 0){
            this.individualMassnahmenMap.putAll(command.getIndividualMassnahmenMap());
        }
        
        return command.getChangedElement();

    }

    /**
     * @param zielobjekt
     * @param bausteineMassnahmenMap
     * @param udBausteineTxtMap
     * @param udBstMassTxtMap
     * @param udBaustGefMap
     * @param b
     */
    private void prepareUserDefinedBausteinImport(NZielobjekt zielobjekt, Map<MbBaust, List<BausteineMassnahmenResult>> bausteineMassnahmenMap, Map<MbBaust, BausteinInformationTransfer> udBausteineTxtMap, Map<MbMassn, MassnahmeInformationTransfer> udBstMassTxtMap, Map<MbBaust, List<GefaehrdungInformationTransfer>> udBaustGefMap, MbBaust b) {
        udBausteineTxtMap.put(b, getGstoolDao().findTxtForMbBaust(b, zielobjekt, GSScraperUtil.getInstance().getModel().getEncoding()));
        List<BausteineMassnahmenResult> lr = bausteineMassnahmenMap.get(b);
        for(BausteineMassnahmenResult r : lr){
            udBstMassTxtMap.put(r.massnahme, getGstoolDao().findTxtforMbMassn(b, r.massnahme, GSScraperUtil.getInstance().getModel().getEncoding()));
        }
        List<GefaehrdungInformationTransfer> gitList = new ArrayList<GefaehrdungInformationTransfer>();
        List<MbBaustGefaehr> mbBaustGefList = getGstoolDao().findGefaehrdungenForBaustein(b, zielobjekt);
        for(MbBaustGefaehr gefaehr : mbBaustGefList){
            GefaehrdungInformationTransfer git = getGstoolDao().findGefaehrdungInformationForBausteinGefaehrdung(b, gefaehr, zielobjekt, GSScraperUtil.getInstance().getModel().getEncoding());
            if(git.getTitel() != null && git.getId() != null){
                gitList.add(git);
            }

        }
        if(gitList.size() > 0){
            udBaustGefMap.put(b, gitList);
        }
    }

    private int getAnzahlMassnahmen(Map<MbBaust, List<BausteineMassnahmenResult>> bausteineMassnahmenMap) {
        Set<MbBaust> keys = bausteineMassnahmenMap.keySet();
        int result = 0;
        for (MbBaust baust : keys) {
            result += bausteineMassnahmenMap.get(baust).size();
        }
        return result;
    }

    /**
     * Import the ergaenzende Sicherheitsanalyse (ESA) for an Zielobjekt.
     * ESA is a synonyme for Risikoanalyse (RA).
     *
     * @param zielobjekt
     * @return
     * @throws CommandException
     */
    private CnATreeElement importEsa(ZielobjektTypeResult zielobjekt, CnATreeElement element) throws CommandException {

        // First transfer the EAS fields into the previously created
        // cnatreeelmt:
        List<ESAResult> esaResult = getGstoolDao().findESAByZielobjekt(zielobjekt.zielobjekt);

        if (esaResult == null || esaResult.size() == 0) {
            LOG.warn("No ESA found for zielobjekt" + zielobjekt.zielobjekt.getName());
            return element;
        }
        if (esaResult.size() > 1) {
            LOG.warn("Warning: More than one ESA found for zielobjekt" + zielobjekt.zielobjekt.getName() + " Using first one only.");
        }

        if(LOG.isDebugEnabled()){
            LOG.debug("ESA found for zielobjekt " + zielobjekt.zielobjekt.getName());
        }
        if (element == null) {
            if(LOG.isDebugEnabled()){
                LOG.debug("No matching CnaTreeElement to migrate ESA for zielobjekt " + zielobjekt.zielobjekt.getName());
            }
            return element;
        }
        transferData.transferESA(element, esaResult.get(0));
        CnAElementHome.getInstance().update(element);
        return element;
    }

    private List<Baustein> loadCatalogueBausteine() throws CommandException, IOException, GSServiceException{
        IBSIConfig bsiConfig = null;
        if (!ServiceFactory.isPermissionHandlingNeeded()) {
            bsiConfig = new BSIConfigurationRCPLocal();
        }
        List<Baustein> bausteine;
        if (bsiConfig == null) {
            // load bausteine from default config:
            LoadBausteine command = new LoadBausteine();
            command = ServiceFactory.lookupCommandService().executeCommand(command);
            bausteine = command.getBausteine();
        } else {
            // load bausteine from given config:
            BSIMassnahmenModel model = GSScraperUtil.getInstance().getModel();
            model.setBSIConfig(bsiConfig);
            bausteine = model.loadBausteine(new sernet.gs.ui.rcp.main.common.model.IProgress() {

                @Override
                public void beginTask(String name, int totalWork) {
                }

                @Override
                public void done() {
                }

                @Override
                public void setTaskName(String string) {
                }

                @Override
                public void subTask(String string) {
                }

                @Override
                public void worked(int work) {
                }
            });
        }
        if(bausteine == null){
            bausteine = new ArrayList<Baustein>(0);
        }
        return bausteine;
    }

    private void copyMDBToTempDB(String sourceDbUrl) {
        Database source = new MDBFileDatabase();
        Database target = new DerbyDatabase();
        try {
            // copy contents of MDB file to temporary derby db:
            String tempDbUrl = CnAWorkspace.getInstance().createTempImportDbUrl();

            DataMover mover = new DataMover();

            source.connect(PreferenceConstants.GS_DB_DRIVER_ODBC, sourceDbUrl);
            target.connect(PreferenceConstants.DB_DRIVER_DERBY, tempDbUrl);

            mover.setSource(source);
            mover.setTarget(target);
            mover.exportDatabse();
        } catch (Exception e) {
            LOG.error("Error: ", e);
            ExceptionUtil.log(e, "Fehler beim Import aus MDB Datei über temporäre Derby-DB.");
        } finally {
            try {
                source.close();
                target.close();
            } catch (Exception e) {
                LOG.debug("Konnte temporäre Import DB nicht schließen.", e);
            }
        }
    }

    public boolean delete(File dir) {
        if (dir.isDirectory()) {
            String[] subdirs = dir.list();
            for (int i = 0; i < subdirs.length; i++) {
                boolean success = delete(new File(dir, subdirs[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    public String getSourceId() {
        return sourceId;
    }
}
