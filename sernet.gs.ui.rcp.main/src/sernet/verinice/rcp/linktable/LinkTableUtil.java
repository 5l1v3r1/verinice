/*******************************************************************************
 * Copyright (c) 2016 Ruth Motza.
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
 *     Ruth Motza <rm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.rcp.linktable;

import java.io.File;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.elasticsearch.common.collect.Sets;

import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.preferences.PreferenceConstants;
import sernet.verinice.rcp.linktable.ui.CsvExportDialog;
import sernet.verinice.rcp.linktable.ui.LinkTableComposite;
import sernet.verinice.rcp.linktable.ui.combo.LinkTableOperationType;
import sernet.verinice.service.csv.ICsvExport;
import sernet.verinice.service.linktable.ColumnPathParseException;
import sernet.verinice.service.linktable.ColumnPathParser;
import sernet.verinice.service.linktable.vlt.VeriniceLinkTable;
import sernet.verinice.service.model.HUIObjectModelLoader;
import sernet.verinice.service.model.ObjectModelValidationException;

/**
 * Util class for {@link LinkTableComposite}
 * 
 * @author Ruth Motza <rm[at]sernet[dot]de>
 */
public class LinkTableUtil {

    private static final Logger LOG = Logger.getLogger(LinkTableUtil.class);
    private static HashMap<String, String> vltExtensions = null;
    private static HashMap<String, String> csvExtensions = null;
    private static CsvExportDialog csvDialog;
    private static HUIObjectModelLoader loader;

    static {
        loader = (HUIObjectModelLoader) HUIObjectModelLoader.getInstance();
        if (vltExtensions == null) {
            vltExtensions = new HashMap<>();
            vltExtensions.put("*" + VeriniceLinkTable.VLT, "verinice link table (.vlt)"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (csvExtensions == null) {
            csvExtensions = new HashMap<>();
            csvExtensions.put("*" + ICsvExport.CSV_FILE_SUFFIX, "CSV table (.csv)"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private LinkTableUtil() {
        // to prevent instantiation
    }

    /**
     * 
     * @param shell
     *            - the shell for the {@link FileDialog}
     * @param text
     *            - the header for the {@link FileDialog}
     * @param defaultFolderPreference
     *            - the id of the defaultFolderpreferences to be updated for the
     *            default directory
     * @param filterExtensions
     *            - the possible extensions to filter in the {@link FileDialog}
     * @param style
     *            - SWT.OPEN or SWT.SAVE
     * @return the absolute filepath to the chosen location
     */
    public static String createFilePath(Shell shell, String text, String defaultFolderPreference,
            Map<String, String> filterExtensions, int style) {
        FileDialog dialog = new FileDialog(shell, style);

        String extension = filterExtensions.keySet().iterator().next().substring(1);
        dialog.setText(text);
        dialog.setFilterPath(getDirectory(defaultFolderPreference));

        ArrayList<String> extensions = new ArrayList<>(filterExtensions.keySet());
        extensions.add("*.*"); //$NON-NLS-1$
        dialog.setFilterExtensions(extensions.toArray(new String[] {})); // $NON-NLS-1$
        ArrayList<String> extensionNames = new ArrayList<>(filterExtensions.values());
        extensionNames.add(Messages.VeriniceLinkTableUtil_1);
        dialog.setFilterNames(extensionNames.toArray(new String[] {}));
        dialog.setFilterIndex(0);
        dialog.setOverwrite(true);

        String filePath = dialog.open();
        if (filePath != null) {

            File file = new File(filePath);
            String dir = file.getParent();
            Activator.getDefault().getPreferenceStore().setValue(defaultFolderPreference, dir);
        }
        if (filePath != null && !filePath.endsWith(extension)) {
            filePath += extension;
        }
        return filePath;
    }

    private static String getDirectory(String defaultFolderPreference) {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        String dir = prefs.getString(defaultFolderPreference);
        if (dir == null || dir.isEmpty()) {
            dir = System.getProperty("user.home"); //$NON-NLS-1$
        }
        if (!dir.endsWith(System.getProperty("file.separator"))) { //$NON-NLS-1$
            dir = dir + System.getProperty("file.separator"); //$NON-NLS-1$
        }
        return dir;
    }

    /**
     * creating a filepath for vlt files
     * 
     * @see #createFilePath(Shell, String, String, Map, int)
     */
    public static String createVltFilePath(Shell shell, String text, int style) {
        return createFilePath(shell, text, PreferenceConstants.DEFAULT_FOLDER_VLT, vltExtensions,
                style);
    }

    /**
     * creating a filepath for vlt files
     * 
     * @see #createFilePath(Shell, String, String, Map, int)
     */
    public static String createCsvFilePath(Shell shell, String text) {
        return createFilePath(shell, text, PreferenceConstants.DEFAULT_FOLDER_CSV_EXPORT,
                csvExtensions, SWT.SAVE);
    }

    /**
     * Returns a filepath but in addition there are all scopes to be chosen for
     * the CSV-Export.
     * 
     * @see CsvExportDialog
     */
    public static String createCsvFilePathAndHandleScopes(Shell shell, String text,
            VeriniceLinkTable veriniceLinkTable) {

        csvDialog = new CsvExportDialog(Display.getCurrent().getActiveShell(), text,
                veriniceLinkTable);
        if (csvDialog.open() == Dialog.OK) {
            return csvDialog.getFilePath();
        }

        return null;

    }

    /**
     * Returns a list of labels for he
     * {@link VeriniceLinkTable#getColumnPaths()}
     * 
     * @param veriniceLinkTable
     * @return
     */
    public static List<String> getTableHeaders(VeriniceLinkTable veriniceLinkTable) {

        ArrayList<String> headers = new ArrayList<>();
        for (String element : veriniceLinkTable.getColumnPaths()) {
            int propertyBeginning = element
                    .lastIndexOf(LinkTableOperationType.PROPERTY.getOutput());
            String propertyId = element.substring(propertyBeginning + 1);
            if (element.contains(LinkTableOperationType.RELATION.getOutput())) {
                headers.add(Messages.getString(propertyId));
            } else {

                headers.add(loader.getLabel(propertyId));
            }
        }

        return headers;

    }

    public static LinkTableValidationResult isValidVeriniceLinkTable(
            VeriniceLinkTable veriniceLinkTable) {

        LinkTableValidationResult result = new LinkTableValidationResult();
        result.setValid(true);
        try {
            validateColumnPathsElements(veriniceLinkTable.getColumnPaths());
            validateRelationIds(veriniceLinkTable.getRelationIds());
            validateRelations(veriniceLinkTable.getColumnPaths());
        } catch (ObjectModelValidationException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e);
            }
            result.setValid(false);
            result.setMessage(e.getMessage());

        }

        return result;
    }

    private static void validateRelations(List<String> columnPaths) throws ObjectModelValidationException {

        Set<Entry<String, String>> relations = getRelations(columnPaths);
        for (Entry<String, String> relation : relations) {
            Set<String> possibleRelationPartners = loader
                    .getPossibleRelationPartners(relation.getKey());
            if (!possibleRelationPartners.contains(relation.getValue())) {
                throw new ObjectModelValidationException("Relation " + relation.toString() + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

    }

    private static void validateColumnPathsElements(List<String> list) throws ObjectModelValidationException {

        for (String path : list) {
            try {
                validateColumnPath(path);
            } catch (Exception e) {
                throw new ObjectModelValidationException(path + " is no valid column path", e);
            }
        }

    }

    public static void validateColumnPath(String path) throws ObjectModelValidationException {
        try {
            ColumnPathParser.throwExceptionIfInvalid(path);
        } catch (ColumnPathParseException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e);
            }
            throw new ObjectModelValidationException(path + " is no valid column path", e); //$NON-NLS-1$
        }
        Set<String> objectTypeIds = ColumnPathParser
                .getObjectTypeIds(Sets.newHashSet(path));
        for (String id : objectTypeIds) {

            boolean validTypeId = loader.isValidTypeId(id);
            if (!validTypeId) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(id + " is no typeId"); //$NON-NLS-1$
                }
                throw new ObjectModelValidationException(validTypeId + " is no valid type ID"); //$NON-NLS-1$
            }
        }
    }

    /**
     * Returns a set of all relations with key- value pairs, where the key is
     * the first typeID and the value the second.
     * 
     */
    public static Set<Entry<String, String>> getRelations(List<String> columnPathes) {

        Set<Entry<String, String>> relations = new HashSet<>();

        for (String path : columnPathes) {
            List<String> pathElements = ColumnPathParser.getColumnPathAsList(path);
            int index = 0;
            for (String element : pathElements) {
                if (LinkTableOperationType.isRelation(element)) {
                    relations.add(new SimpleEntry<String, String>(pathElements.get(index - 1),
                            pathElements.get(index + 1)));

                }
                index++;
            }

        }

        return relations;
    }

    private static void validateRelationIds(List<String> list) throws ObjectModelValidationException {

        for (String id : list) {
            if (!loader.isValidRelationId(id)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(id + " is no RelationID"); //$NON-NLS-1$
                }
                throw new ObjectModelValidationException(id + " is no valid relation id"); //$NON-NLS-1$
            }
        }
    }
}
